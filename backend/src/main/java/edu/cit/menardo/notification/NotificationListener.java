package edu.cit.menardo.notification;

import java.util.UUID;

import edu.cit.menardo.inventory.events.LowStockEvent;
import edu.cit.menardo.shop.events.OrderCancelledEvent;
import edu.cit.menardo.shop.events.OrderPlacedEvent;
import edu.cit.menardo.shop.events.OrderRejectedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Subscribes to domain events and writes the activity log.
 *
 * The only things this module imports from the others are the event records.
 * It never calls the Order or Inventory services, and neither of them knows
 * this class exists - Spring's event bus is the only connection.
 *
 * Listeners are synchronous (no @Async): they run on the publisher's thread,
 * inside the publisher's transaction. See the README for why.
 */
@Component
class NotificationListener {

    private final NotificationRepository repository;

    NotificationListener(NotificationRepository repository) {
        this.repository = repository;
    }

    @EventListener
    void on(OrderPlacedEvent event) {
        save(NotificationRecord.ORDER_CONFIRMED, "Order %s confirmed (%s, %d %s)."
                .formatted(shortId(event.orderId()),
                        plural(event.lineCount(), "product"),
                        event.totalQuantity(), event.totalQuantity() == 1 ? "unit" : "units"));
    }

    @EventListener
    void on(OrderRejectedEvent event) {
        save(NotificationRecord.ORDER_REJECTED, "Order %s rejected: %s"
                .formatted(shortId(event.orderId()), event.reason()));
    }

    @EventListener
    void on(OrderCancelledEvent event) {
        save(NotificationRecord.ORDER_CANCELLED, "Order %s cancelled, %d %s returned to stock."
                .formatted(shortId(event.orderId()),
                        event.totalQuantity(), event.totalQuantity() == 1 ? "unit" : "units"));
    }

    @EventListener
    void on(LowStockEvent event) {
        save(NotificationRecord.LOW_STOCK, "Reorder needed: %s (%s) is down to %d, below the threshold of %d."
                .formatted(event.name(), event.productId(), event.remainingStock(), event.threshold()));
    }

    private void save(String type, String message) {
        repository.save(new NotificationRecord(type, message));
    }

    /** First block of the UUID - enough to tell orders apart in the feed. */
    static String shortId(UUID orderId) {
        return orderId == null ? "?" : orderId.toString().substring(0, 8);
    }

    private static String plural(int count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }
}
