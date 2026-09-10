package edu.cit.menardo.inventory;

/**
 * The only shape of inventory data that leaves this module.
 * The JPA entity stays package-private, so the Order module cannot touch
 * a managed entity or accidentally write to the inventory table.
 */
public record InventoryView(String productId, String name, int stock) {
}
