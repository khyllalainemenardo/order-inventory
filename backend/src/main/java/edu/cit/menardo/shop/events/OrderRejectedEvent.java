package edu.cit.menardo.shop.events;

import java.util.UUID;

/** Published by OrderService when an order is rejected and nothing was reserved. */
public record OrderRejectedEvent(UUID orderId, String reason) {
}
