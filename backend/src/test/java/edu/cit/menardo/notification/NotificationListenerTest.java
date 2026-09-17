package edu.cit.menardo.notification;

import java.lang.reflect.Modifier;
import java.util.UUID;

import edu.cit.menardo.inventory.events.LowStockEvent;
import edu.cit.menardo.shop.events.OrderCancelledEvent;
import edu.cit.menardo.shop.events.OrderPlacedEvent;
import edu.cit.menardo.shop.events.OrderRejectedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationListenerTest {

    private static final UUID ID = UUID.fromString("7a9c4303-15ce-4f6b-a69b-46caa2b5e001");

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationListener listener = new NotificationListener(repository);

    @Test
    void confirmedOrder() {
        listener.on(new OrderPlacedEvent(ID, 2, 9));
        assertSaved("ORDER_CONFIRMED", "Order 7a9c4303 confirmed (2 products, 9 units).");
    }

    @Test
    void rejectedOrder() {
        listener.on(new OrderRejectedEvent(ID, "Only 0 of USB-C Hub left, 1 requested."));
        assertSaved("ORDER_REJECTED", "Order 7a9c4303 rejected: Only 0 of USB-C Hub left, 1 requested.");
    }

    @Test
    void cancelledOrder() {
        listener.on(new OrderCancelledEvent(ID, 2, 9));
        assertSaved("ORDER_CANCELLED", "Order 7a9c4303 cancelled, 9 units returned to stock.");
    }

    @Test
    void lowStockIsItsOwnKindOfEntry() {
        listener.on(new LowStockEvent("P200", "Mechanical Keyboard", 4, 5));
        assertSaved("LOW_STOCK", "Reorder needed: Mechanical Keyboard (P200) is down to 4, below the threshold of 5.");
    }

    @Test
    void internalsAreNotPublic() {
        assertThat(Modifier.isPublic(NotificationListener.class.getModifiers())).isFalse();
        assertThat(Modifier.isPublic(NotificationRecord.class.getModifiers())).isFalse();
        assertThat(Modifier.isPublic(NotificationRepository.class.getModifiers())).isFalse();
    }

    private void assertSaved(String type, String message) {
        ArgumentCaptor<NotificationRecord> saved = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(type);
        assertThat(saved.getValue().getMessage()).isEqualTo(message);
    }
}
