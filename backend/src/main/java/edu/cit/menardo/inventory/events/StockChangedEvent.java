package edu.cit.menardo.inventory.events;

public record StockChangedEvent(String productId, int change, int stock) {
}
