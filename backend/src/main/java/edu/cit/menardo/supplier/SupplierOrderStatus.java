package edu.cit.menardo.supplier;

import java.util.List;

public enum SupplierOrderStatus {
    PENDING,
    PLACED,
    PICKING,
    SHIPPED,
    DELIVERED,
    FAILED,
    NEEDS_REVIEW;

    public static final List<SupplierOrderStatus> OPEN = List.of(PENDING, PLACED, PICKING, SHIPPED);

    public static final List<SupplierOrderStatus> TRACKED = List.of(PLACED, PICKING, SHIPPED);
}
