package edu.cit.menardo.shop;

import java.util.List;
import java.util.UUID;

import edu.cit.menardo.inventory.InventoryView;

/**
 * POST /api/orders response: { orderId, status, reason, items, inventory }.
 *
 * @param items     one outcome per line
 * @param inventory current state of every product the order touched
 */
public record OrderResponse(UUID orderId,
                            String status,
                            String reason,
                            List<ItemOutcome> items,
                            List<InventoryView> inventory) {

    /** RESERVED, INSUFFICIENT_STOCK, or NOT_RESERVED (fine on its own, skipped because another line failed). */
    public record ItemOutcome(String productId, int quantity, String outcome) {

        public static final String RESERVED = "RESERVED";
        public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
        public static final String NOT_RESERVED = "NOT_RESERVED";
    }
}
