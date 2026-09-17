package edu.cit.menardo.shop;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cit.menardo.inventory.InventoryService;
import edu.cit.menardo.inventory.InventoryView;
import edu.cit.menardo.shop.OrderExceptions.InvalidOrderException;
import edu.cit.menardo.shop.OrderExceptions.OrderNotFoundException;
import edu.cit.menardo.shop.OrderExceptions.OrderStateException;
import edu.cit.menardo.shop.OrderExceptions.ReservationConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns the Order module's exceptions into JSON bodies with the right status code. */
@RestControllerAdvice(assignableTypes = OrderController.class)
class OrderErrorHandler {

    private final InventoryService inventoryService;

    OrderErrorHandler(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @ExceptionHandler(InvalidOrderException.class)
    ResponseEntity<ApiError> invalid(InvalidOrderException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "Body must be { \"items\": [{ \"productId\", \"quantity\" }] }.");
    }

    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<ApiError> notFound(OrderNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(OrderStateException.class)
    ResponseEntity<ApiError> conflict(OrderStateException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * The transaction has already rolled back by the time this runs, so stock is
     * read fresh. Answered like any other rejection (200, status REJECTED); the
     * attempt is not stored because the rollback removed it with the reservations.
     */
    @ExceptionHandler(ReservationConflictException.class)
    OrderResponse raced(ReservationConflictException e) {
        Set<String> touched = new HashSet<>();
        e.items().forEach(item -> touched.add(item.productId()));
        List<InventoryView> inventory = inventoryService.listItems().stream()
                .filter(item -> touched.contains(item.productId()))
                .toList();
        return new OrderResponse(null, OrderRecord.REJECTED, e.getMessage(), e.items(), inventory);
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), message));
    }
}
