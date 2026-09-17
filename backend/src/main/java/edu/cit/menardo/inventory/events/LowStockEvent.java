package edu.cit.menardo.inventory.events;

public record LowStockEvent(String productId, String name, int remainingStock, int threshold) {
}
