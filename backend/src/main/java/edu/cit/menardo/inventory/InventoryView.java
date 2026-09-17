package edu.cit.menardo.inventory;

public record InventoryView(String productId, String name, int stock, boolean lowStock) {
}
