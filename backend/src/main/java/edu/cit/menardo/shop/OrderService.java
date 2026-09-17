package edu.cit.menardo.shop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import edu.cit.menardo.inventory.InventoryService;
import edu.cit.menardo.inventory.InventoryView;
import edu.cit.menardo.inventory.ReservationResult;
import edu.cit.menardo.shop.OrderExceptions.InvalidOrderException;
import edu.cit.menardo.shop.OrderExceptions.OrderNotFoundException;
import edu.cit.menardo.shop.OrderExceptions.OrderStateException;
import edu.cit.menardo.shop.OrderExceptions.ReservationConflictException;
import edu.cit.menardo.shop.OrderResponse.ItemOutcome;
import edu.cit.menardo.shop.events.OrderCancelledEvent;
import edu.cit.menardo.shop.events.OrderPlacedEvent;
import edu.cit.menardo.shop.events.OrderRejectedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration point of the monolith.
 *
 * Talks to Inventory through the InventoryService interface only, and tells the
 * rest of the application what happened by publishing events - it never calls
 * the Notification module and does not know it exists.
 */
@Service
public class OrderService {

    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher events;

    OrderService(InventoryService inventoryService,
                 OrderRepository orderRepository,
                 ApplicationEventPublisher events) {
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
        this.events = events;
    }

    /**
     * All-or-nothing. Every line is checked against current stock first; only if
     * all of them fit is anything reserved. One transaction covers the
     * reservations, the order row, its line items and (because the listeners run
     * synchronously) the activity-log rows.
     */
    @Transactional
    public OrderResponse place(OrderRequest request) {
        Map<String, Integer> lines = normalise(request);

        // 1. Validate every line before touching stock.
        Map<String, InventoryView> before = new LinkedHashMap<>();
        List<String> shortages = new ArrayList<>();
        for (Map.Entry<String, Integer> line : lines.entrySet()) {
            InventoryView item = inventoryService.getItem(line.getKey())
                    .orElseThrow(() -> new InvalidOrderException("No product with id " + line.getKey() + "."));
            before.put(item.productId(), item);
            if (item.stock() < line.getValue()) {
                shortages.add("Only %d of %s left, %d requested."
                        .formatted(item.stock(), item.name(), line.getValue()));
            }
        }

        if (!shortages.isEmpty()) {
            return reject(lines, before, String.join(" ", shortages));
        }

        // 2. Everything fits - reserve each line.
        Map<String, InventoryView> after = new LinkedHashMap<>(before);
        for (Map.Entry<String, Integer> line : lines.entrySet()) {
            ReservationResult result = inventoryService.reserve(line.getKey(), line.getValue());
            if (!result.confirmed()) {
                // Another order took the stock between validation and reservation.
                // Throwing rolls back the lines already reserved in this transaction.
                throw new ReservationConflictException(
                        "Stock changed while the order was being placed. " + result.reason(),
                        conflictOutcomes(lines, line.getKey()));
            }
            after.put(line.getKey(), result.inventory());
        }

        OrderRecord order = new OrderRecord(OrderRecord.CONFIRMED, null);
        lines.forEach(order::addItem);
        order = orderRepository.save(order);

        events.publishEvent(new OrderPlacedEvent(order.getOrderId(), lines.size(), total(lines)));

        List<ItemOutcome> outcomes = lines.entrySet().stream()
                .map(line -> new ItemOutcome(line.getKey(), line.getValue(), ItemOutcome.RESERVED))
                .toList();
        return new OrderResponse(order.getOrderId(), OrderRecord.CONFIRMED, null,
                outcomes, List.copyOf(after.values()));
    }

    /** Order history, newest first. */
    @Transactional(readOnly = true)
    public List<OrderSummary> history() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(OrderSummary::of)
                .toList();
    }

    /**
     * Cancels a confirmed order and returns every line to stock.
     * 404 if the order does not exist, 409 if it is already cancelled or was
     * rejected (a rejected order never reserved anything, so there is nothing to return).
     */
    @Transactional
    public OrderSummary cancel(String orderId) {
        OrderRecord order = parseId(orderId)
                .flatMap(orderRepository::findById)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (OrderRecord.CANCELLED.equals(order.getStatus())) {
            throw new OrderStateException("Order " + orderId + " is already cancelled.");
        }
        if (OrderRecord.REJECTED.equals(order.getStatus())) {
            throw new OrderStateException(
                    "Order " + orderId + " was rejected, so there is no reserved stock to return.");
        }

        int totalQuantity = 0;
        for (OrderItemRecord item : order.getItems()) {
            inventoryService.restock(item.getProductId(), item.getQuantity());
            totalQuantity += item.getQuantity();
        }
        order.cancel();

        events.publishEvent(new OrderCancelledEvent(order.getOrderId(), order.getItems().size(), totalQuantity));
        return OrderSummary.of(order);
    }

    // --- helpers -------------------------------------------------------------

    private OrderResponse reject(Map<String, Integer> lines,
                                 Map<String, InventoryView> stock,
                                 String reason) {
        OrderRecord order = new OrderRecord(OrderRecord.REJECTED, reason);
        lines.forEach(order::addItem);
        order = orderRepository.save(order);

        events.publishEvent(new OrderRejectedEvent(order.getOrderId(), reason));

        List<ItemOutcome> outcomes = lines.entrySet().stream()
                .map(line -> new ItemOutcome(line.getKey(), line.getValue(),
                        stock.get(line.getKey()).stock() < line.getValue()
                                ? ItemOutcome.INSUFFICIENT_STOCK
                                : ItemOutcome.NOT_RESERVED))
                .toList();
        return new OrderResponse(order.getOrderId(), OrderRecord.REJECTED, reason,
                outcomes, List.copyOf(stock.values()));
    }

    /**
     * Checks the request shape and merges duplicate products, keeping the order
     * in which they were first added.
     */
    private static Map<String, Integer> normalise(OrderRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new InvalidOrderException("Add at least one item before ordering.");
        }
        Map<String, Integer> lines = new LinkedHashMap<>();
        for (OrderRequest.Item item : request.items()) {
            if (item == null || item.productId() == null || item.productId().isBlank()) {
                throw new InvalidOrderException("Every item needs a productId.");
            }
            if (item.quantity() == null || item.quantity() < 1) {
                throw new InvalidOrderException("Quantity for " + item.productId().trim() + " must be at least 1.");
            }
            lines.merge(item.productId().trim(), item.quantity(), Integer::sum);
        }
        return lines;
    }

    private static List<ItemOutcome> conflictOutcomes(Map<String, Integer> lines, String failedProductId) {
        return lines.entrySet().stream()
                .map(line -> new ItemOutcome(line.getKey(), line.getValue(),
                        line.getKey().equals(failedProductId)
                                ? ItemOutcome.INSUFFICIENT_STOCK
                                : ItemOutcome.NOT_RESERVED))
                .toList();
    }

    private static int total(Map<String, Integer> lines) {
        return lines.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static Optional<UUID> parseId(String orderId) {
        try {
            return Optional.of(UUID.fromString(orderId));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }
}
