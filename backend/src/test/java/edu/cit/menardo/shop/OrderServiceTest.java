package edu.cit.menardo.shop;

import java.util.List;
import java.util.Optional;

import edu.cit.menardo.inventory.InventoryService;
import edu.cit.menardo.inventory.InventoryView;
import edu.cit.menardo.inventory.ReservationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * No database and no Spring context. Because Order depends on the interface
 * only, a hand-written stub is enough to test both paths.
 */
class OrderServiceTest {

    private final OrderRepository orders = mock(OrderRepository.class);

    @Test
    void confirmsWhenStockIsAvailable() {
        InventoryService inventory = stub(ReservationResult.confirmed(
                new InventoryView("P100", "Wireless Mouse", 23)));

        OrderResponse response = new OrderService(inventory, orders)
                .place(new OrderRequest("P100", 2));

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.reason()).isNull();
        assertThat(response.inventory().stock()).isEqualTo(23);
        verify(orders, times(1)).save(any(OrderRecord.class));
    }

    @Test
    void rejectsWhenStockIsShort() {
        InventoryService inventory = stub(ReservationResult.rejected(
                "Only 0 of USB-C Hub left, 1 requested.",
                new InventoryView("P300", "USB-C Hub", 0)));

        OrderResponse response = new OrderService(inventory, orders)
                .place(new OrderRequest("P300", 1));

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.reason()).contains("USB-C Hub");
        assertThat(response.inventory().stock()).isZero();
    }

    @Test
    void recordsRejectedAttemptsToo() {
        InventoryService inventory = stub(ReservationResult.rejected("nope", null));

        new OrderService(inventory, orders).place(new OrderRequest("P300", 5));

        verify(orders, times(1)).save(any(OrderRecord.class));
    }

    private InventoryService stub(ReservationResult result) {
        return new InventoryService() {
            @Override
            public List<InventoryView> listItems() {
                return List.of();
            }

            @Override
            public Optional<InventoryView> getItem(String productId) {
                return Optional.ofNullable(result.inventory());
            }

            @Override
            public ReservationResult reserve(String productId, int quantity) {
                return result;
            }
        };
    }
}
