package edu.cit.menardo.supplier.events;

public record SupplierOrderStopped(String reference, String productId, String reason) {
}
