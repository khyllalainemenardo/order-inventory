package edu.cit.menardo.supplier;

import java.time.Duration;
import java.util.Optional;

import edu.cit.menardo.supplier.events.SupplierOrderPlaced;
import edu.cit.menardo.supplier.events.SupplierOrderStopped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class ReorderSender {

    private static final Logger log = LoggerFactory.getLogger(ReorderSender.class);

    private static final Duration FRESH = Duration.ofMinutes(1);

    private final SupplierOrderRepository repository;
    private final LegacySupplyClient client;
    private final LegacySupplyTranslator translator;
    private final ApplicationEventPublisher events;

    ReorderSender(SupplierOrderRepository repository,
                  LegacySupplyClient client,
                  LegacySupplyTranslator translator,
                  ApplicationEventPublisher events) {
        this.repository = repository;
        this.client = client;
        this.translator = translator;
        this.events = events;
    }

    @Async
    @TransactionalEventListener
    public void onReorderSaved(ReorderSaved event) {
        send(event.supplierOrderId());
    }

    synchronized boolean send(Long supplierOrderId) {
        SupplierOrder order = repository.findById(supplierOrderId).orElse(null);
        if (order == null || order.getStatus() != SupplierOrderStatus.PENDING) {
            return true;
        }

        try {
            LegacyOrderAck ack = findEarlierAttempt(order)
                    .orElseGet(() -> client.placeOrder(
                            translator.supplierSku(order.getProductId()),
                            order.getCases(),
                            order.getBuyerRef(),
                            order.getRequestId()));

            order.markPlaced(ack.poNumber());
            repository.save(order);
            log.info("Reorder {} placed as {}", order.getBuyerRef(), ack.poNumber());
            events.publishEvent(new SupplierOrderPlaced(order.getBuyerRef(), order.getProductId(), order.getUnits()));
            return true;
        } catch (LegacySupplyException e) {
            if (e.isRejected()) {
                order.changeStatus(SupplierOrderStatus.FAILED);
                repository.save(order);
                events.publishEvent(new SupplierOrderStopped(order.getBuyerRef(), order.getProductId(),
                        "the supplier refused the order"));
                return true;
            }
            log.warn("Reorder {} stays PENDING: {}", order.getBuyerRef(), e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Reorder {} stays PENDING, unreadable reply: {}", order.getBuyerRef(), e.getMessage());
            return false;
        }
    }

    private Optional<LegacyOrderAck> findEarlierAttempt(SupplierOrder order) {
        if (!order.waitingLongerThan(FRESH)) {
            return Optional.empty();
        }
        return client.findOrderByBuyerRef(order.getBuyerRef());
    }
}
