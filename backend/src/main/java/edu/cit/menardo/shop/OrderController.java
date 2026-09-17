package edu.cit.menardo.shop;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SuppressWarnings("unused")
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    OrderResponse placeOrder(@RequestBody OrderRequest request) {
        return orderService.place(request);
    }

    @GetMapping
    List<OrderSummary> history() {
        return orderService.history();
    }

    @PostMapping("/{orderId}/cancel")
    OrderSummary cancel(@PathVariable String orderId) {
        return orderService.cancel(orderId);
    }

    @ExceptionHandler(OrderException.class)
    ResponseEntity<ApiError> handleOrderError(OrderException e) {
        return error(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleBadBody() {
        return error(HttpStatus.BAD_REQUEST, "Body must be { \"items\": [{ \"productId\", \"quantity\" }] }.");
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), message));
    }
}
