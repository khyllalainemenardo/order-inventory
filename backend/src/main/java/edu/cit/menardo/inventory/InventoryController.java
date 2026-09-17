package edu.cit.menardo.inventory;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/inventory - live stock for the dashboard. Moved here from the Order
 * module in Lab 2, since inventory data belongs to the Inventory module.
 */
@RestController
@RequestMapping("/api")
class InventoryController {

    private final InventoryService inventoryService;

    InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/inventory")
    List<InventoryView> inventory() {
        return inventoryService.listItems();
    }
}
