package edu.cit.menardo.inventory;

/**
 * Outcome of a reservation attempt.
 *
 * @param confirmed true when stock was actually deducted
 * @param reason    null when confirmed, otherwise why it was refused
 * @param inventory the item's state after the attempt, or null if the product is unknown
 */
public record ReservationResult(boolean confirmed, String reason, InventoryView inventory) {

    public static ReservationResult confirmed(InventoryView inventory) {
        return new ReservationResult(true, null, inventory);
    }

    public static ReservationResult rejected(String reason, InventoryView inventory) {
        return new ReservationResult(false, reason, inventory);
    }
}
