package edu.cit.menardo.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Package-private on purpose: the inventory table is this module's private state.
 */
@Entity
@Table(name = "inventory")
class InventoryItem {

    @Id
    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "stock", nullable = false)
    private int stock;

    protected InventoryItem() {
        // required by JPA
    }

    /** For tests only - rows are created by db/schema.sql. */
    InventoryItem(String productId, String name, int stock) {
        this.productId = productId;
        this.name = name;
        this.stock = stock;
    }

    String getProductId() {
        return productId;
    }

    String getName() {
        return name;
    }

    int getStock() {
        return stock;
    }

    InventoryView toView(int lowStockThreshold) {
        return new InventoryView(productId, name, stock, stock < lowStockThreshold);
    }
}
