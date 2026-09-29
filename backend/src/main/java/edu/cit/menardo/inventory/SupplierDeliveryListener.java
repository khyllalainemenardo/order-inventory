package edu.cit.menardo.inventory;

import edu.cit.menardo.supplier.events.SupplierOrderDelivered;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class SupplierDeliveryListener {

    private final InventoryService inventoryService;

    SupplierDeliveryListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @EventListener
    void on(SupplierOrderDelivered event) {
        inventoryService.restock(event.productId(), event.units());
    }
}
