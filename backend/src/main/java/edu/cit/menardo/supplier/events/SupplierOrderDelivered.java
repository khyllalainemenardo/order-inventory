package edu.cit.menardo.supplier.events;

public record SupplierOrderDelivered(String reference, String productId, int units) {
}
