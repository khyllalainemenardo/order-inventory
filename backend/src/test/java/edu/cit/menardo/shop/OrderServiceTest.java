package edu.cit.menardo.shop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import edu.cit.menardo.inventory.InventoryService;
import edu.cit.menardo.inventory.InventoryView;
import edu.cit.menardo.inventory.ReservationResult;
import edu.cit.menardo.shop.OrderResponse.ItemOutcome;
import edu.cit.menardo.shop.events.OrderCancelledEvent;
import edu.cit.menardo.shop.events.OrderPlacedEvent;
import edu.cit.menardo.shop.events.OrderRejectedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private FakeInventory inventory;
    private OrderService service;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        when(orders.save(any(OrderRecord.class))).thenAnswer(call -> call.getArgument(0));
        inventory = new FakeInventory(Map.of(
                "P100", 25,
                "P200", 10,
                "P300", 0));
        service = new OrderService(inventory, orders, events);
    }

    @Test
    void confirmsWhenEveryLineHasStock() {
        OrderResponse response = service.place(order(line("P100", 3), line("P200", 6)));

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.reason()).isNull();
        assertThat(response.items()).extracting(ItemOutcome::outcome)
                .containsExactly(ItemOutcome.RESERVED, ItemOutcome.RESERVED);
        assertThat(inventory.stock("P100")).isEqualTo(22);
        assertThat(inventory.stock("P200")).isEqualTo(4);
        assertThat(inventory.reserveCalls).isEqualTo(2);
        verify(events).publishEvent(any(OrderPlacedEvent.class));
        verify(orders).save(any(OrderRecord.class));
    }

    @Test
    void rejectsTheWholeOrderWhenOneLineIsShort() {
        OrderResponse response = service.place(order(line("P100", 2), line("P300", 1)));

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.reason()).contains("USB-C Hub");
        assertThat(response.items()).extracting(ItemOutcome::outcome)
                .containsExactly(ItemOutcome.NOT_RESERVED, ItemOutcome.INSUFFICIENT_STOCK);
        assertThat(inventory.reserveCalls).as("nothing reserved when any line fails").isZero();
        assertThat(inventory.stock("P100")).isEqualTo(25);
        verify(events).publishEvent(any(OrderRejectedEvent.class));
        verify(events, never()).publishEvent(any(OrderPlacedEvent.class));
    }

    @Test
    void recordsRejectedOrdersWithTheirLines() {
        service.place(order(line("P100", 2), line("P300", 1)));

        verify(orders).save(org.mockito.ArgumentMatchers.<OrderRecord>argThat(saved ->
                saved.getStatus().equals("REJECTED") && saved.getItems().size() == 2));
    }

    @Test
    void mergesDuplicateLinesBeforeValidating() {
        OrderResponse response = service.place(order(line("P200", 6), line("P200", 6)));

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(12);
        assertThat(inventory.reserveCalls).isZero();
    }

    @Test
    void rejectsMalformedRequests() {
        assertThatThrownBy(() -> service.place(new OrderRequest(List.of())))
                .isInstanceOf(OrderException.class);
        assertThatThrownBy(() -> service.place(order(line("P100", 0))))
                .isInstanceOf(OrderException.class);
        assertThatThrownBy(() -> service.place(order(line(" ", 1))))
                .isInstanceOf(OrderException.class);
        assertThatThrownBy(() -> service.place(order(line("P999", 1))))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("P999");
        assertThat(inventory.reserveCalls).isZero();
    }

    @Test
    void throwsSoTheTransactionRollsBackWhenStockChangesMidOrder() {
        inventory.failReserveFor.add("P200");

        assertThatThrownBy(() -> service.place(order(line("P100", 1), line("P200", 1))))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("Stock changed");
        verify(orders, never()).save(any(OrderRecord.class));
    }

    @Test
    void cancelReturnsEveryLineToStock() {
        OrderRecord confirmed = storedOrder("CONFIRMED", Map.of("P100", 3, "P200", 6));

        OrderSummary summary = service.cancel(confirmed.getOrderId() + "");

        assertThat(summary.status()).isEqualTo("CANCELLED");
        assertThat(inventory.stock("P100")).isEqualTo(28);
        assertThat(inventory.stock("P200")).isEqualTo(16);
        assertThat(inventory.restockCalls).isEqualTo(2);
        verify(events).publishEvent(any(OrderCancelledEvent.class));
    }

    @Test
    void cancelUnknownOrderIsNotFound() {
        assertThatThrownBy(() -> service.cancel(UUID.randomUUID().toString()))
                .isInstanceOf(OrderException.class);
        assertThatThrownBy(() -> service.cancel("not-a-uuid"))
                .isInstanceOf(OrderException.class);
    }

    @Test
    void cancelTwiceIsAConflict() {
        OrderRecord order = storedOrder("CONFIRMED", Map.of("P100", 1));
        service.cancel(order.getOrderId().toString());

        assertThatThrownBy(() -> service.cancel(order.getOrderId().toString()))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("already cancelled");
        assertThat(inventory.restockCalls).isEqualTo(1);
    }

    @Test
    void cancelRejectedOrderIsAConflictAndRestocksNothing() {
        OrderRecord rejected = storedOrder("REJECTED", Map.of("P300", 1));

        assertThatThrownBy(() -> service.cancel(rejected.getOrderId().toString()))
                .isInstanceOf(OrderException.class);
        assertThat(inventory.restockCalls).isZero();
    }

    private static OrderRequest order(OrderRequest.Item... lines) {
        return new OrderRequest(List.of(lines));
    }

    private static OrderRequest.Item line(String productId, int quantity) {
        return new OrderRequest.Item(productId, quantity);
    }

    private OrderRecord storedOrder(String status, Map<String, Integer> lines) {
        UUID id = UUID.randomUUID();
        OrderRecord order = new OrderRecord(status, null) {
            @Override
            UUID getOrderId() {
                return id;
            }
        };
        new java.util.TreeMap<>(lines).forEach(order::addItem);
        when(orders.findById(id)).thenReturn(Optional.of(order));
        return order;
    }

    private static class FakeInventory implements InventoryService {

        private final Map<String, Integer> stock = new LinkedHashMap<>();
        final Set<String> failReserveFor = new HashSet<>();
        int reserveCalls;
        int restockCalls;

        FakeInventory(Map<String, Integer> initial) {
            stock.putAll(new java.util.TreeMap<>(initial));
        }

        int stock(String productId) {
            return stock.get(productId);
        }

        private InventoryView view(String productId) {
            int s = stock.get(productId);
            return new InventoryView(productId, name(productId), s, s < 5);
        }

        private static String name(String productId) {
            return switch (productId) {
                case "P100" -> "Wireless Mouse";
                case "P200" -> "Mechanical Keyboard";
                default -> "USB-C Hub";
            };
        }

        @Override
        public List<InventoryView> listItems() {
            return new ArrayList<>(stock.keySet()).stream().map(this::view).toList();
        }

        @Override
        public Optional<InventoryView> getItem(String productId) {
            return stock.containsKey(productId) ? Optional.of(view(productId)) : Optional.empty();
        }

        @Override
        public ReservationResult reserve(String productId, int quantity) {
            reserveCalls++;
            if (failReserveFor.contains(productId) || stock.get(productId) < quantity) {
                return ReservationResult.rejected("Only 0 left.", view(productId));
            }
            stock.merge(productId, -quantity, Integer::sum);
            return ReservationResult.confirmed(view(productId));
        }

        @Override
        public InventoryView restock(String productId, int quantity) {
            restockCalls++;
            stock.merge(productId, quantity, Integer::sum);
            return view(productId);
        }
    }
}
