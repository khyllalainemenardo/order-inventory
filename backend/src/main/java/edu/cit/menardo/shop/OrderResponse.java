package edu.cit.menardo.shop;

import edu.cit.menardo.inventory.InventoryView;

/** POST /api/orders response: { status, reason, inventory }. */
public record OrderResponse(String status, String reason, InventoryView inventory) {
}
