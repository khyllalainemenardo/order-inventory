package edu.cit.menardo.supplier;

public record ReorderResult(String reference, String productId, int unitsOrdered, SupplierOrderStatus status) {
}
