package edu.cit.menardo.supplier.events;

public record SupplierOrderPlaced(String reference, String productId, int units) {
}
