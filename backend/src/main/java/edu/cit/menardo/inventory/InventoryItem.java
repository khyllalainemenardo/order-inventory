package edu.cit.menardo.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory")
class InventoryItem {

    @Id
    @Column(name = "product_id")
    private String productId;

    private String name;

    private int stock;

    protected InventoryItem() {
    }

    InventoryItem(String productId, String name, int stock) {
        this.productId = productId;
        this.name = name;
        this.stock = stock;
    }

    InventoryView toView(int lowStockThreshold) {
        return new InventoryView(productId, name, stock, stock < lowStockThreshold);
    }
}
