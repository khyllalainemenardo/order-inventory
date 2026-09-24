package edu.cit.menardo.inventory;

public record ReservationResult(boolean confirmed, String reason, InventoryView inventory) {

    public static ReservationResult confirmed(InventoryView inventory) {
        return new ReservationResult(true, null, inventory);
    }

    public static ReservationResult rejected(String reason, InventoryView inventory) {
        return new ReservationResult(false, reason, inventory);
    }
}
