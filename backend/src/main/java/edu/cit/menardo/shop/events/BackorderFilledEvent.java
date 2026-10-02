package edu.cit.menardo.shop.events;

import java.util.UUID;

public record BackorderFilledEvent(UUID orderId) {
}
