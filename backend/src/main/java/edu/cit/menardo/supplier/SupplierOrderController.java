package edu.cit.menardo.supplier;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SuppressWarnings("unused")
class SupplierOrderController {

    record SupplierOrderView(String reference, String productId, int units, SupplierOrderStatus status,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    private final SupplierOrderRepository repository;

    SupplierOrderController(SupplierOrderRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/supplier-orders")
    List<SupplierOrderView> supplierOrders() {
        return repository.findTop50ByOrderByIdDesc().stream()
                .map(order -> new SupplierOrderView(order.getBuyerRef(), order.getProductId(), order.getUnits(),
                        order.getStatus(), order.getCreatedAt(), order.getUpdatedAt()))
                .toList();
    }
}
