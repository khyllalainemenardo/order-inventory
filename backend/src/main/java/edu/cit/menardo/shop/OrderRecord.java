package edu.cit.menardo.shop;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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

@Entity
@Table(name = "orders")
class OrderRecord {

    static final String CONFIRMED = "CONFIRMED";
    static final String REJECTED = "REJECTED";
    static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private UUID orderId;

    private String status;

    private String reason;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("id")
    private final List<OrderItemRecord> items = new ArrayList<>();

    protected OrderRecord() {
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
        return items;
    }
}
