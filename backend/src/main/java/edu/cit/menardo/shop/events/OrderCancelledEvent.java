package edu.cit.menardo.shop.events;

import java.util.UUID;

public record OrderCancelledEvent(UUID orderId, int lineCount, int totalQuantity) {
}
