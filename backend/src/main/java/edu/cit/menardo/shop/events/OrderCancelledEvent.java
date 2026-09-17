package edu.cit.menardo.shop.events;

import java.util.UUID;

/** Published by OrderService after a confirmed order is cancelled and its stock returned. */
public record OrderCancelledEvent(UUID orderId, int lineCount, int totalQuantity) {
}
