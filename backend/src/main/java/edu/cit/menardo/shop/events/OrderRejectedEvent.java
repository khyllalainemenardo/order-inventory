package edu.cit.menardo.shop.events;

import java.util.UUID;

public record OrderRejectedEvent(UUID orderId, String reason) {
}
