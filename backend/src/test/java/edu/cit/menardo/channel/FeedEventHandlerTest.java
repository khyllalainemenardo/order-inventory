package edu.cit.menardo.channel;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import edu.cit.menardo.channel.TianggeJson.FeedEvent;
import edu.cit.menardo.channel.TianggeJson.Line;
import edu.cit.menardo.shop.OrderResponse;
import edu.cit.menardo.shop.OrderService;
import edu.cit.menardo.shop.OrderSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedEventHandlerTest {

    private static final Set<String> LISTED = Set.of("P100", "P300");

    private final OrderService orderService = mock(OrderService.class);
    private final ChannelOrderRepository orders = mock(ChannelOrderRepository.class);
    private final ProcessedEventRepository processedEvents = mock(ProcessedEventRepository.class);
    private final FeedCursorRepository cursors = mock(FeedCursorRepository.class);
    private FeedEventHandler handler;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        TransactionTemplate transaction = new TransactionTemplate(mock(PlatformTransactionManager.class));
        handler = new FeedEventHandler(orderService, new TianggeTranslator(), orders, processedEvents, cursors, transaction);
        when(cursors.findById(FeedCursor.ID)).thenReturn(Optional.of(new FeedCursor(40)));
    }

    @Test
    void aNewOrderBecomesOneOrderInOurSystemAndADecision() {
        UUID ours = UUID.randomUUID();
        when(orderService.placeAllowingBackorder(any())).thenReturn(response(ours, "CONFIRMED"));

        FeedEventHandler.Reply reply = handler.handle(placed(41, "evt_1", "TG-1", "P100", 2), LISTED);

        assertThat(reply).isEqualTo(FeedEventHandler.Reply.DECISION);
        ArgumentCaptor<ChannelOrder> saved = ArgumentCaptor.forClass(ChannelOrder.class);
        verify(orders).save(saved.capture());
        assertThat(saved.getValue().getDecision()).isEqualTo(ChannelDecision.ACCEPTED);
        assertThat(saved.getValue().getShopOrderId()).isEqualTo(ours);
        verify(processedEvents).save(any(ProcessedEvent.class));
        ArgumentCaptor<FeedCursor> cursor = ArgumentCaptor.forClass(FeedCursor.class);
        verify(cursors).save(cursor.capture());
        assertThat(cursor.getValue().getLastSeq()).isEqualTo(41);
    }

    @Test
    void aRedeliveredEventIsSkippedButTheCursorStillMoves() {
        when(processedEvents.existsById("evt_1")).thenReturn(true);

        FeedEventHandler.Reply reply = handler.handle(placed(57, "evt_1", "TG-1", "P100", 2), LISTED);

        assertThat(reply).isEqualTo(FeedEventHandler.Reply.NONE);
        verify(orderService, never()).placeAllowingBackorder(any());
        ArgumentCaptor<FeedCursor> cursor = ArgumentCaptor.forClass(FeedCursor.class);
        verify(cursors).save(cursor.capture());
        assertThat(cursor.getValue().getLastSeq()).isEqualTo(57);
    }

    @Test
    void anOrderAlreadyTakenUnderAnotherEventIdIsNotPlacedAgain() {
        when(orders.existsById("TG-1")).thenReturn(true);

        handler.handle(placed(42, "evt_2", "TG-1", "P100", 2), LISTED);

        verify(orderService, never()).placeAllowingBackorder(any());
    }

    @Test
    void backorderedAndRejectedOrdersAreReportedAsSuch() {
        when(orderService.placeAllowingBackorder(any()))
                .thenReturn(response(UUID.randomUUID(), "BACKORDERED"))
                .thenReturn(response(UUID.randomUUID(), "REJECTED"));
        ArgumentCaptor<ChannelOrder> saved = ArgumentCaptor.forClass(ChannelOrder.class);

        handler.handle(placed(41, "evt_1", "TG-1", "P300", 1), LISTED);
        handler.handle(placed(42, "evt_2", "TG-2", "P300", 1), LISTED);

        verify(orders, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(ChannelOrder::getDecision)
                .containsExactly(ChannelDecision.BACKORDERED, ChannelDecision.REJECTED);
    }

    @Test
    void anOrderForAnUnlistedProductIsRejectedWithoutTouchingInventory() {
        FeedEventHandler.Reply reply = handler.handle(placed(41, "evt_1", "TG-1", "P999", 1), LISTED);

        assertThat(reply).isEqualTo(FeedEventHandler.Reply.DECISION);
        verify(orderService, never()).placeAllowingBackorder(any());
    }

    @Test
    void aCustomerCancellationCancelsOurOrderOnce() {
        UUID ours = UUID.randomUUID();
        ChannelOrder taken = ChannelOrder.decided("TG-1", ours, ChannelDecision.ACCEPTED, null);
        when(orders.findById("TG-1")).thenReturn(Optional.of(taken));
        when(orderService.find(ours)).thenReturn(Optional.of(
                new OrderSummary(ours, "CONFIRMED", null, null, List.of())));

        FeedEventHandler.Reply reply = handler.handle(cancelled(43, "evt_3", "TG-1"), LISTED);
        FeedEventHandler.Reply again = handler.handle(cancelled(44, "evt_4", "TG-1"), LISTED);

        assertThat(reply).isEqualTo(FeedEventHandler.Reply.CANCELLATION);
        assertThat(again).isEqualTo(FeedEventHandler.Reply.NONE);
        verify(orderService).cancel(ours.toString());
        assertThat(taken.isCancelReceived()).isTrue();
    }

    @Test
    void aCancellationForAnAlreadyCancelledOrderIsConfirmedWithoutCancellingAgain() {
        UUID ours = UUID.randomUUID();
        when(orders.findById("TG-1")).thenReturn(Optional.of(
                ChannelOrder.decided("TG-1", ours, ChannelDecision.BACKORDERED, null)));
        when(orderService.find(ours)).thenReturn(Optional.of(
                new OrderSummary(ours, "CANCELLED", null, null, List.of())));

        assertThat(handler.handle(cancelled(43, "evt_3", "TG-1"), LISTED))
                .isEqualTo(FeedEventHandler.Reply.CANCELLATION);
        verify(orderService, never()).cancel(anyString());
    }

    private static FeedEvent placed(long seq, String eventId, String orderId, String sku, int qty) {
        return new FeedEvent(seq, eventId, "ORDER_PLACED", orderId, null, null, null, null, List.of(new Line(sku, qty)));
    }

    private static FeedEvent cancelled(long seq, String eventId, String orderId) {
        return new FeedEvent(seq, eventId, "ORDER_CANCELLED", orderId, null, null, null, null, null);
    }

    private static OrderResponse response(UUID orderId, String status) {
        return new OrderResponse(orderId, status, null, List.of(), List.of());
    }
}
