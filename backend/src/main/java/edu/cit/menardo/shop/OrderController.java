package edu.cit.menardo.shop;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Returns 200 for both CONFIRMED and REJECTED. A rejection is a valid business
     * answer, not a broken request, and the front-end reads the status field either way.
     */
    @PostMapping
    OrderResponse placeOrder(@RequestBody OrderRequest request) {
        return orderService.place(request);
    }

    @GetMapping
    List<OrderSummary> history() {
        return orderService.history();
    }

    /** 200 with the cancelled order, 404 if unknown, 409 if already cancelled or rejected. */
    @PostMapping("/{orderId}/cancel")
    OrderSummary cancel(@PathVariable String orderId) {
        return orderService.cancel(orderId);
    }
}
