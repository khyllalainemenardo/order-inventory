package edu.cit.menardo.shop;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps the orders table. Named OrderRecord because "order" is a reserved word
 * in SQL and a confusing class name next to java.util.
 */
@Entity
@Table(name = "orders")
class OrderRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected OrderRecord() {
        // required by JPA
    }

    OrderRecord(String productId, int quantity, String status, String reason) {
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.reason = reason;
        this.createdAt = OffsetDateTime.now();
    }

    UUID getOrderId() {
        return orderId;
    }

    String getStatus() {
        return status;
    }
}
