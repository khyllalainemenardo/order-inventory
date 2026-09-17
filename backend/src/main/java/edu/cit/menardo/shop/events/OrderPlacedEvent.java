package edu.cit.menardo.shop.events;

import java.util.UUID;

/** Published by OrderService when an order is confirmed and all its lines are reserved. */
public record OrderPlacedEvent(UUID orderId, int lineCount, int totalQuantity) {
}
