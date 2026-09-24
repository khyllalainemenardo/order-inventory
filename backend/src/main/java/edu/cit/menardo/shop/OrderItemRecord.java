package edu.cit.menardo.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
@SuppressWarnings("unused")
class OrderItemRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderRecord order;

    @Column(name = "product_id")
    private String productId;

    private int quantity;

    protected OrderItemRecord() {
    }

    OrderItemRecord(OrderRecord order, String productId, int quantity) {
        this.order = order;
        this.productId = productId;
        this.quantity = quantity;
    }

    String getProductId() {
        return productId;
    }

    int getQuantity() {
        return quantity;
    }
}
