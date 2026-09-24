package edu.cit.menardo.supplier;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class SupplierJobs {

    private final SupplierOrderRepository repository;
    private final ReorderSender sender;
    private final DeliveryTracker tracker;

    SupplierJobs(SupplierOrderRepository repository, ReorderSender sender, DeliveryTracker tracker) {
        this.repository = repository;
        this.sender = sender;
        this.tracker = tracker;
    }

    @Scheduled(initialDelay = 15_000, fixedDelayString = "${supplier.send-pending-every-ms}")
    public void sendPendingReorders() {
        for (SupplierOrder order : repository.findByStatusOrderByIdAsc(SupplierOrderStatus.PENDING)) {
            boolean supplierAnswered = sender.send(order.getId());
            if (!supplierAnswered) {
                return;
            }
        }
    }

    @Scheduled(initialDelay = 30_000, fixedDelayString = "${supplier.track-deliveries-every-ms}")
    public void trackOpenOrders() {
        for (SupplierOrder order : repository.findByStatusInOrderByIdAsc(SupplierOrderStatus.TRACKED)) {
            boolean supplierAnswered = tracker.check(order);
            if (!supplierAnswered) {
                return;
            }
        }
    }
}
