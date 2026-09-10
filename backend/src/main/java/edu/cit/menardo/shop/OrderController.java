package edu.cit.menardo.shop;

import java.util.List;

import edu.cit.menardo.inventory.InventoryService;
import edu.cit.menardo.inventory.InventoryView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class OrderController {

    private final OrderService orderService;
    private final InventoryService inventoryService;

    OrderController(OrderService orderService, InventoryService inventoryService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
    }

    /**
     * Returns 200 for both outcomes. A rejection is a valid business answer,
     * not a broken request, and the front-end reads the status field either way.
     */
    @PostMapping("/orders")
    OrderResponse placeOrder(@RequestBody OrderRequest request) {
        return orderService.place(request);
    }

    /** Feeds the product dropdown so the front-end has no hard-coded catalogue. */
    @GetMapping("/inventory")
    List<InventoryView> inventory() {
        return inventoryService.listItems();
    }
}
