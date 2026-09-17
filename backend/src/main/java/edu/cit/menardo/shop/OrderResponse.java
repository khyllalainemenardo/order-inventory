package edu.cit.menardo.shop;

import java.util.List;
import java.util.UUID;

import edu.cit.menardo.inventory.InventoryView;

public record OrderResponse(UUID orderId,
                            String status,
                            String reason,
                            List<ItemOutcome> items,
                            List<InventoryView> inventory) {

    public record ItemOutcome(String productId, int quantity, String outcome) {

        public static final String RESERVED = "RESERVED";
        public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
        public static final String NOT_RESERVED = "NOT_RESERVED";
    }
}
