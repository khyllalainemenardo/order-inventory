package edu.cit.menardo.supplier;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
class LegacySupplyTranslator {

    private static final int MAX_CASES = 99;

    private record CatalogItem(String supplierSku, int packSize) {
    }

    private static final Map<String, CatalogItem> CATALOG = Map.of(
            "P100", new CatalogItem("WLU-6392", 24),
            "P200", new CatalogItem("WLU-9391", 24),
            "P300", new CatalogItem("WLU-5564", 10),
            "P400", new CatalogItem("WLU-7311", 10),
            "P500", new CatalogItem("WLU-8644", 6));

    boolean knows(String productId) {
        return CATALOG.containsKey(productId);
    }

    String supplierSku(String productId) {
        return CATALOG.get(productId).supplierSku();
    }

    int casesFor(String productId, int unitsNeeded) {
        int packSize = CATALOG.get(productId).packSize();
        int cases = (unitsNeeded + packSize - 1) / packSize;
        return Math.max(1, Math.min(cases, MAX_CASES));
    }

    int unitsIn(String productId, int cases) {
        return cases * CATALOG.get(productId).packSize();
    }

    SupplierOrderStatus toStatus(int statusCode) {
        return switch (statusCode) {
            case 10 -> SupplierOrderStatus.PLACED;
            case 20 -> SupplierOrderStatus.PICKING;
            case 30 -> SupplierOrderStatus.SHIPPED;
            case 40 -> SupplierOrderStatus.DELIVERED;
            default -> SupplierOrderStatus.NEEDS_REVIEW;
        };
    }
}
