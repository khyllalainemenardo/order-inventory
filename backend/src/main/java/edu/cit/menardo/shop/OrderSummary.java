package edu.cit.menardo.shop;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** One entry of GET /api/orders, also returned by the cancel endpoint. */
public record OrderSummary(UUID orderId,
                           String status,
                           String reason,
                           OffsetDateTime createdAt,
                           List<Line> items) {

    public record Line(String productId, int quantity) {
    }

    static OrderSummary of(OrderRecord order) {
        List<Line> lines = order.getItems().stream()
                .map(item -> new Line(item.getProductId(), item.getQuantity()))
                .toList();
        return new OrderSummary(order.getOrderId(), order.getStatus(), order.getReason(),
                order.getCreatedAt(), lines);
    }
}
