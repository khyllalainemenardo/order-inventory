package edu.cit.menardo.supplier;

import edu.cit.menardo.supplier.events.SupplierOrderDelivered;
import edu.cit.menardo.supplier.events.SupplierOrderStopped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class DeliveryTracker {

    private static final Logger log = LoggerFactory.getLogger(DeliveryTracker.class);

    private final SupplierOrderRepository repository;
    private final LegacySupplyClient client;
    private final LegacySupplyTranslator translator;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;

    DeliveryTracker(SupplierOrderRepository repository,
                    LegacySupplyClient client,
                    LegacySupplyTranslator translator,
                    ApplicationEventPublisher events,
                    TransactionTemplate transaction) {
        this.repository = repository;
        this.client = client;
        this.translator = translator;
        this.events = events;
        this.transaction = transaction;
    }

    boolean check(SupplierOrder order) {
        LegacyOrderAck ack;
        try {
            ack = client.getOrder(order.getPoNumber());
        } catch (LegacySupplyException | IllegalArgumentException e) {
            log.warn("Could not check {}: {}", order.getBuyerRef(), e.getMessage());
            return false;
        }

        SupplierOrderStatus newStatus = translator.toStatus(ack.statusCode());
        if (newStatus == order.getStatus()) {
            return true;
        }
        if (newStatus == SupplierOrderStatus.NEEDS_REVIEW) {
            log.warn("{} ({}) has unexpected supplier status code {}", order.getBuyerRef(), ack.poNumber(), ack.statusCode());
        }

        transaction.executeWithoutResult(status -> applyStatus(order.getId(), newStatus));
        return true;
    }

    private void applyStatus(Long supplierOrderId, SupplierOrderStatus newStatus) {
        SupplierOrder order = repository.findById(supplierOrderId).orElseThrow();
        order.changeStatus(newStatus);
        log.info("{} is now {}", order.getBuyerRef(), newStatus);

        if (newStatus == SupplierOrderStatus.DELIVERED) {
            events.publishEvent(new SupplierOrderDelivered(order.getBuyerRef(), order.getProductId(), order.getUnits()));
        }
        if (newStatus == SupplierOrderStatus.NEEDS_REVIEW) {
            events.publishEvent(new SupplierOrderStopped(order.getBuyerRef(), order.getProductId(),
                    "the supplier reported a status we do not recognise, so no stock is expected"));
        }
    }
}
