package edu.cit.menardo.shop.events;

import java.util.UUID;

public record OrderPlacedEvent(UUID orderId, int lineCount, int totalQuantity) {
}
