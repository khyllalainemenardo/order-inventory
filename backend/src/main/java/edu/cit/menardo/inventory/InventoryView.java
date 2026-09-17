package edu.cit.menardo.inventory;

/**
 * The only shape of inventory data that leaves this module.
 * The JPA entity stays package-private, so no other module can touch
 * a managed entity or accidentally write to the inventory table.
 *
 * @param lowStock true when stock is below the configured reorder threshold
 */
public record InventoryView(String productId, String name, int stock, boolean lowStock) {
}
