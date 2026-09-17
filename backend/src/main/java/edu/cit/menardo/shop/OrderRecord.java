package edu.cit.menardo.shop;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Maps the orders table. Named OrderRecord because "order" is a reserved word
 * in SQL and a confusing class name next to java.util.
 *
 * Since Lab 2 the product and quantity live on the line items.
 */
@Entity
@Table(name = "orders")
class OrderRecord {

    static final String CONFIRMED = "CONFIRMED";
    static final String REJECTED = "REJECTED";
    static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItemRecord> items = new ArrayList<>();

    protected OrderRecord() {
        // required by JPA
    }

    OrderRecord(String status, String reason) {
        this.status = status;
        this.reason = reason;
        this.createdAt = OffsetDateTime.now();
    }

    void addItem(String productId, int quantity) {
        items.add(new OrderItemRecord(this, productId, quantity));
    }

    void cancel() {
        this.status = CANCELLED;
    }

    UUID getOrderId() {
        return orderId;
    }

    String getStatus() {
        return status;
    }

    String getReason() {
        return reason;
    }

    OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    List<OrderItemRecord> getItems() {
        return Collections.unmodifiableList(items);
    }
}
