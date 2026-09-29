package edu.cit.menardo.supplier;

public interface SupplierGateway {

    ReorderResult requestReorder(String productId, int unitsNeeded);
}
