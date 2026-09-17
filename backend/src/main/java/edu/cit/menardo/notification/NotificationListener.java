package edu.cit.menardo.notification;

import java.util.UUID;

import edu.cit.menardo.inventory.events.LowStockEvent;
import edu.cit.menardo.shop.events.OrderCancelledEvent;
import edu.cit.menardo.shop.events.OrderPlacedEvent;
import edu.cit.menardo.shop.events.OrderRejectedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class NotificationListener {

    private final NotificationRepository repository;

    NotificationListener(NotificationRepository repository) {
        this.repository = repository;
    }

    @EventListener
    void on(OrderPlacedEvent event) {
        save("ORDER_CONFIRMED", "Order " + shortId(event.orderId()) + " confirmed ("
                + plural(event.lineCount(), "product") + ", " + plural(event.totalQuantity(), "unit") + ").");
    }

    @EventListener
    void on(OrderRejectedEvent event) {
        save("ORDER_REJECTED", "Order " + shortId(event.orderId()) + " rejected: " + event.reason());
    }

    @EventListener
    void on(OrderCancelledEvent event) {
        save("ORDER_CANCELLED", "Order " + shortId(event.orderId()) + " cancelled, "
                + plural(event.totalQuantity(), "unit") + " returned to stock.");
    }

    @EventListener
    void on(LowStockEvent event) {
        save("LOW_STOCK", "Reorder needed: " + event.name() + " (" + event.productId() + ") is down to "
                + event.remainingStock() + ", below the threshold of " + event.threshold() + ".");
    }

    private void save(String type, String message) {
        repository.save(new NotificationRecord(type, message));
    }

    private static String shortId(UUID orderId) {
        return orderId.toString().substring(0, 8);
    }

    private static String plural(int count, String word) {
        return count + " " + word + (count == 1 ? "" : "s");
    }
}
