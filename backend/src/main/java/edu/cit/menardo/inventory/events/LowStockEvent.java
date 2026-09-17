package edu.cit.menardo.inventory.events;

/**
 * Published by the Inventory module after a successful reservation leaves a
 * product below the reorder threshold.
 */
public record LowStockEvent(String productId, String name, int remainingStock, int threshold) {
}
