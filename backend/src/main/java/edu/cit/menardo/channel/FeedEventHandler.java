package edu.cit.menardo.channel;

import java.util.Optional;
import java.util.Set;

import edu.cit.menardo.channel.TianggeJson.FeedEvent;
import edu.cit.menardo.shop.OrderRequest;
import edu.cit.menardo.shop.OrderResponse;
import edu.cit.menardo.shop.OrderService;
import edu.cit.menardo.shop.OrderSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class FeedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(FeedEventHandler.class);

    enum Reply { NONE, DECISION, CANCELLATION }

    private final OrderService orderService;
    private final TianggeTranslator translator;
    private final ChannelOrderRepository orders;
    private final ProcessedEventRepository processedEvents;
    private final FeedCursorRepository cursors;
    private final TransactionTemplate transaction;

    FeedEventHandler(OrderService orderService,
                     TianggeTranslator translator,
                     ChannelOrderRepository orders,
                     ProcessedEventRepository processedEvents,
                     FeedCursorRepository cursors,
                     TransactionTemplate transaction) {
        this.orderService = orderService;
        this.translator = translator;
        this.orders = orders;
        this.processedEvents = processedEvents;
        this.cursors = cursors;
        this.transaction = transaction;
    }

    long cursor() {
        return cursors.findById(FeedCursor.ID).map(FeedCursor::getLastSeq).orElse(0L);
    }

    Reply handle(FeedEvent event, Set<String> listedSkus) {
        return transaction.execute(status -> {
            Reply reply = Reply.NONE;
            if (processedEvents.existsById(event.eventId())) {
                log.info("Skipping redelivered event {} (seq {}, {} {})", event.eventId(), event.seq(), event.type(), event.orderId());
            } else {
                reply = switch (event.type()) {
                    case TianggeTranslator.ORDER_PLACED -> orderPlaced(event, listedSkus);
                    case TianggeTranslator.ORDER_CANCELLED -> orderCancelled(event);
                    default -> {
                        log.info("Ignoring feed event type {} (seq {})", event.type(), event.seq());
                        yield Reply.NONE;
                    }
                };
                processedEvents.save(new ProcessedEvent(event.eventId(), event.seq(), event.type(), event.orderId()));
            }
            moveCursor(event.seq());
            return reply;
        });
    }

    void skip(FeedEvent event) {
        transaction.executeWithoutResult(status -> {
            if (!processedEvents.existsById(event.eventId())) {
                processedEvents.save(new ProcessedEvent(event.eventId(), event.seq(), event.type() + "_SKIPPED", event.orderId()));
            }
            moveCursor(event.seq());
        });
    }

    private Reply orderPlaced(FeedEvent event, Set<String> listedSkus) {
        if (orders.existsById(event.orderId())) {
            log.info("Order {} already taken under another event, skipping {}", event.orderId(), event.eventId());
            return Reply.NONE;
        }

        Optional<OrderRequest> request = translator.toOrderRequest(event, listedSkus);
        if (request.isEmpty()) {
            orders.save(ChannelOrder.decided(event.orderId(), null, ChannelDecision.REJECTED,
                    "Order names a product this shop does not sell."));
            log.warn("Tiangge order {} rejected: unknown products {}", event.orderId(), event.lines());
            return Reply.DECISION;
        }

        OrderResponse placed = orderService.placeAllowingBackorder(request.get());
        ChannelDecision decision = translator.toDecision(placed.status());
        orders.save(ChannelOrder.decided(event.orderId(), placed.orderId(), decision, translator.toReason(placed.reason())));
        log.info("Tiangge order {} {} -> our order {} {} ({})", event.orderId(), event.lines(), placed.orderId(),
                placed.status(), decision);
        return Reply.DECISION;
    }

    private Reply orderCancelled(FeedEvent event) {
        ChannelOrder order = orders.findById(event.orderId()).orElse(null);
        if (order == null) {
            orders.save(ChannelOrder.unknownCancelled(event.orderId()));
            log.warn("Cancellation for Tiangge order {} that we never took; confirming it", event.orderId());
            return Reply.CANCELLATION;
        }
        if (order.isCancelReceived()) {
            return Reply.NONE;
        }

        if (order.getShopOrderId() != null) {
            String status = orderService.find(order.getShopOrderId()).map(OrderSummary::status).orElse("");
            if (status.equals("CONFIRMED") || status.equals("BACKORDERED")) {
                orderService.cancel(order.getShopOrderId().toString());
                log.info("Tiangge order {} cancelled by the buyer -> our order {} cancelled and restocked",
                        event.orderId(), order.getShopOrderId());
            } else {
                log.info("Tiangge order {} cancelled by the buyer; our order {} was already {}",
                        event.orderId(), order.getShopOrderId(), status);
            }
        }
        order.cancelReceived();
        orders.save(order);
        return Reply.CANCELLATION;
    }

    private void moveCursor(long seq) {
        FeedCursor cursor = cursors.findById(FeedCursor.ID).orElseGet(() -> new FeedCursor(0));
        cursor.moveTo(seq);
        cursors.save(cursor);
    }
}
