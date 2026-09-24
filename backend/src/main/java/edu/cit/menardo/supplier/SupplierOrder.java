package edu.cit.menardo.supplier;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "supplier_orders")
class SupplierOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "buyer_ref")
    private String buyerRef;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "po_number")
    private String poNumber;

    private int cases;

    private int units;

    @Enumerated(EnumType.STRING)
    private SupplierOrderStatus status;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected SupplierOrder() {
    }

    SupplierOrder(String productId, int cases, int units) {
        this.productId = productId;
        this.cases = cases;
        this.units = units;
        this.requestId = UUID.randomUUID().toString();
        this.status = SupplierOrderStatus.PENDING;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    void assignBuyerRef() {
        this.buyerRef = "RO-" + id;
    }

    void markPlaced(String poNumber) {
        this.poNumber = poNumber;
        changeStatus(SupplierOrderStatus.PLACED);
    }

    void changeStatus(SupplierOrderStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = OffsetDateTime.now();
    }

    boolean waitingLongerThan(Duration duration) {
        return createdAt.plus(duration).isBefore(OffsetDateTime.now());
    }

    ReorderResult toResult() {
        return new ReorderResult(buyerRef, productId, units, status);
    }

    Long getId() {
        return id;
    }

    String getProductId() {
        return productId;
    }

    String getBuyerRef() {
        return buyerRef;
    }

    String getRequestId() {
        return requestId;
    }

    String getPoNumber() {
        return poNumber;
    }

    int getCases() {
        return cases;
    }

    int getUnits() {
        return units;
    }

    SupplierOrderStatus getStatus() {
        return status;
    }

    OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
