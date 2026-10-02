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
import edu.cit.menardo.shop.OrderResponse.ItemOutcome;
import edu.cit.menardo.shop.events.OrderBackorderedEvent;
import edu.cit.menardo.shop.events.OrderCancelledEvent;
import edu.cit.menardo.shop.events.OrderPlacedEvent;
import edu.cit.menardo.shop.events.OrderRejectedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public OrderResponse place(OrderRequest request) {
        return place(request, false);
    }

    @Transactional
    public OrderResponse placeAllowingBackorder(OrderRequest request) {
        return place(request, true);
    }

    private OrderResponse place(OrderRequest request, boolean allowBackorder) {
        Map<String, Integer> lines = readLines(request);

        Map<String, InventoryView> stock = new LinkedHashMap<>();
        List<String> shortages = new ArrayList<>();
        for (String productId : lines.keySet()) {
            InventoryView item = inventoryService.getItem(productId)
                    .orElseThrow(() -> new OrderException(HttpStatus.BAD_REQUEST, "No product with id " + productId + "."));
            stock.put(productId, item);

            int wanted = lines.get(productId);
            if (item.stock() < wanted) {
                shortages.add("Only " + item.stock() + " of " + item.name() + " left, " + wanted + " requested.");
            }
        }

        if (!shortages.isEmpty()) {
            String reason = String.join(" ", shortages);
            if (allowBackorder && coveredByIncomingStock(lines, stock)) {
                return backorder(lines, stock, reason);
            }
            reportShortages(lines, stock);
            return reject(lines, stock, reason);
        }

        for (String productId : lines.keySet()) {
            ReservationResult result = inventoryService.reserve(productId, lines.get(productId));
            if (!result.confirmed()) {
                throw new OrderException(HttpStatus.CONFLICT, "Stock changed while the order was being placed. Please try again.");
            }
            stock.put(productId, result.inventory());
        }

        OrderRecord order = saveOrder(OrderRecord.CONFIRMED, null, lines);
        events.publishEvent(new OrderPlacedEvent(order.getOrderId(), lines.size(), totalQuantity(lines)));

        List<ItemOutcome> outcomes = new ArrayList<>();
        lines.forEach((productId, quantity) -> outcomes.add(new ItemOutcome(productId, quantity, ItemOutcome.RESERVED)));
        return new OrderResponse(order.getOrderId(), OrderRecord.CONFIRMED, null, outcomes, List.copyOf(stock.values()));
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> history() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(OrderSummary::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<OrderSummary> find(UUID orderId) {
        return orderRepository.findById(orderId).map(OrderSummary::of);
    }

    @Transactional
    public OrderSummary cancel(String orderId) {
        OrderRecord order = findOrderForUpdate(orderId);

        if (order.getStatus().equals(OrderRecord.CANCELLED)) {
            throw new OrderException(HttpStatus.CONFLICT, "Order " + orderId + " is already cancelled.");
        }
        if (order.getStatus().equals(OrderRecord.REJECTED)) {
            throw new OrderException(HttpStatus.CONFLICT, "Order " + orderId + " was rejected, so there is no stock to return.");
        }

        boolean stockWasReserved = order.getStatus().equals(OrderRecord.CONFIRMED);
        List<OrderItemRecord> items = List.copyOf(order.getItems());
        // Change the status before touching inventory: its update queries flush and then clear the
        // persistence context, so a change made to the order afterwards would never be saved.
        order.cancel();
        int totalQuantity = 0;
        for (OrderItemRecord item : items) {
            if (stockWasReserved) {
                inventoryService.restock(item.getProductId(), item.getQuantity());
                totalQuantity += item.getQuantity();
            }
        }

        events.publishEvent(new OrderCancelledEvent(order.getOrderId(), items.size(), totalQuantity));
        return OrderSummary.of(order);
    }

    private boolean coveredByIncomingStock(Map<String, Integer> lines, Map<String, InventoryView> stock) {
        Map<String, Integer> alreadyPromised = backorderedQuantities();
        for (Map.Entry<String, Integer> line : lines.entrySet()) {
            String productId = line.getKey();
            int onHand = stock.get(productId).stock();
            if (onHand >= line.getValue()) {
                continue;
            }
            int incoming = inventoryService.incomingUnits(productId);
            int promised = alreadyPromised.getOrDefault(productId, 0);
            if (incoming == 0 || onHand + incoming - promised < line.getValue()) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Integer> backorderedQuantities() {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (OrderRecord order : orderRepository.findByStatusOrderByCreatedAtAsc(OrderRecord.BACKORDERED)) {
            for (OrderItemRecord item : order.getItems()) {
                quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }
        return quantities;
    }

    private void reportShortages(Map<String, Integer> lines, Map<String, InventoryView> stock) {
        lines.forEach((productId, quantity) -> {
            if (stock.get(productId).stock() < quantity) {
                inventoryService.reportShortage(productId, quantity);
            }
        });
    }

    private OrderResponse backorder(Map<String, Integer> lines, Map<String, InventoryView> stock, String reason) {
        OrderRecord order = saveOrder(OrderRecord.BACKORDERED, reason, lines);
        events.publishEvent(new OrderBackorderedEvent(order.getOrderId()));

        List<ItemOutcome> outcomes = new ArrayList<>();
        lines.forEach((productId, quantity) -> outcomes.add(new ItemOutcome(productId, quantity, ItemOutcome.NOT_RESERVED)));
        return new OrderResponse(order.getOrderId(), OrderRecord.BACKORDERED, reason, outcomes, List.copyOf(stock.values()));
    }

    private OrderResponse reject(Map<String, Integer> lines, Map<String, InventoryView> stock, String reason) {
        OrderRecord order = saveOrder(OrderRecord.REJECTED, reason, lines);
        events.publishEvent(new OrderRejectedEvent(order.getOrderId(), reason));

        List<ItemOutcome> outcomes = new ArrayList<>();
        lines.forEach((productId, quantity) -> {
            boolean shortOfStock = stock.get(productId).stock() < quantity;
            String outcome = shortOfStock ? ItemOutcome.INSUFFICIENT_STOCK : ItemOutcome.NOT_RESERVED;
            outcomes.add(new ItemOutcome(productId, quantity, outcome));
        });
        return new OrderResponse(order.getOrderId(), OrderRecord.REJECTED, reason, outcomes, List.copyOf(stock.values()));
    }

    private OrderRecord saveOrder(String status, String reason, Map<String, Integer> lines) {
        OrderRecord order = new OrderRecord(status, reason);
        lines.forEach(order::addItem);
        return orderRepository.save(order);
    }

    private OrderRecord findOrderForUpdate(String orderId) {
        try {
            return orderRepository.findForUpdate(UUID.fromString(orderId))
                    .orElseThrow(() -> new OrderException(HttpStatus.NOT_FOUND, "No order with id " + orderId + "."));
        } catch (IllegalArgumentException notAUuid) {
            throw new OrderException(HttpStatus.NOT_FOUND, "No order with id " + orderId + ".");
        }
    }

    private static Map<String, Integer> readLines(OrderRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new OrderException(HttpStatus.BAD_REQUEST, "Add at least one item before ordering.");
        }

        Map<String, Integer> lines = new LinkedHashMap<>();
        for (OrderRequest.Item item : request.items()) {
            if (item == null || item.productId() == null || item.productId().isBlank()) {
                throw new OrderException(HttpStatus.BAD_REQUEST, "Every item needs a productId.");
            }
            if (item.quantity() == null || item.quantity() < 1) {
                throw new OrderException(HttpStatus.BAD_REQUEST, "Quantity for " + item.productId() + " must be at least 1.");
            }
            lines.merge(item.productId().trim(), item.quantity(), Integer::sum);
        }
        return lines;
    }

    private static int totalQuantity(Map<String, Integer> lines) {
        int total = 0;
        for (int quantity : lines.values()) {
            total += quantity;
        }
        return total;
    }
}
