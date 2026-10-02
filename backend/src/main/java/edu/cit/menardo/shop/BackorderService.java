package edu.cit.menardo.shop;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import edu.cit.menardo.inventory.InventoryService;
import edu.cit.menardo.inventory.InventoryView;
import edu.cit.menardo.inventory.events.StockChangedEvent;
import edu.cit.menardo.shop.events.BackorderCancelledEvent;
import edu.cit.menardo.shop.events.BackorderFilledEvent;
import edu.cit.menardo.shop.events.OrderPlacedEvent;
import edu.cit.menardo.supplier.events.SupplierOrderStopped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class BackorderService {

    private static final Logger log = LoggerFactory.getLogger(BackorderService.class);

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;
    private final Duration maxWait;

    BackorderService(OrderRepository orderRepository,
                     InventoryService inventoryService,
                     ApplicationEventPublisher events,
                     TransactionTemplate transaction,
                     @Value("${orders.backorder-max-wait-seconds}") long maxWaitSeconds) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.events = events;
        this.transaction = transaction;
        this.maxWait = Duration.ofSeconds(maxWaitSeconds);
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onStockChanged(StockChangedEvent event) {
        if (event.change() > 0) {
            reviewBackorders();
        }
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onSupplierOrderStopped(SupplierOrderStopped event) {
        reviewBackorders();
    }

    @Scheduled(initialDelay = 20_000, fixedDelayString = "${orders.backorder-review-every-ms}")
    public void reviewRegularly() {
        reviewBackorders();
    }

    synchronized void reviewBackorders() {
        List<UUID> waiting = orderRepository.findByStatusOrderByCreatedAtAsc(OrderRecord.BACKORDERED).stream()
                .map(OrderRecord::getOrderId)
                .toList();
        for (UUID orderId : waiting) {
            try {
                transaction.executeWithoutResult(status -> review(orderId, status));
            } catch (RuntimeException e) {
                log.warn("Could not review backorder {}: {}", orderId, e.getMessage());
            }
        }
    }

    private void review(UUID orderId, TransactionStatus status) {
        OrderRecord order = orderRepository.findForUpdate(orderId).orElse(null);
        if (order == null || !order.getStatus().equals(OrderRecord.BACKORDERED)) {
            return;
        }

        if (everyLineInStock(order)) {
            List<OrderItemRecord> items = List.copyOf(order.getItems());
            // Confirm before reserving: inventory's update queries flush and then clear the persistence
            // context, so a confirm made afterwards would be lost and the order filled again next review.
            // If a reservation fails, the rollback undoes the confirm too.
            order.confirm();
            for (OrderItemRecord item : items) {
                if (!inventoryService.reserve(item.getProductId(), item.getQuantity()).confirmed()) {
                    status.setRollbackOnly();
                    return;
                }
            }
            int units = items.stream().mapToInt(OrderItemRecord::getQuantity).sum();
            log.info("Backorder {} filled", orderId);
            events.publishEvent(new OrderPlacedEvent(orderId, items.size(), units));
            events.publishEvent(new BackorderFilledEvent(orderId));
            return;
        }

        String reason = whyItCannotWait(order);
        if (reason != null) {
            order.cancel(reason);
            log.info("Backorder {} cancelled: {}", orderId, reason);
            events.publishEvent(new BackorderCancelledEvent(orderId, reason));
        }
    }

    private boolean everyLineInStock(OrderRecord order) {
        for (OrderItemRecord item : order.getItems()) {
            int onHand = inventoryService.getItem(item.getProductId()).map(InventoryView::stock).orElse(0);
            if (onHand < item.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    private String whyItCannotWait(OrderRecord order) {
        if (order.getCreatedAt().plus(maxWait).isBefore(OffsetDateTime.now())) {
            return "Supplier delivery did not arrive in time.";
        }
        for (OrderItemRecord item : order.getItems()) {
            int onHand = inventoryService.getItem(item.getProductId()).map(InventoryView::stock).orElse(0);
            if (onHand < item.getQuantity() && inventoryService.incomingUnits(item.getProductId()) == 0) {
                return "Not enough " + item.getProductId() + " after the supplier delivery.";
            }
        }
        return null;
    }
}
