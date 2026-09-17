package edu.cit.menardo.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface InventoryRepository extends JpaRepository<InventoryItem, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InventoryItem i set i.stock = i.stock - :quantity where i.productId = :productId and i.stock >= :quantity")
    int deductIfAvailable(String productId, int quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InventoryItem i set i.stock = i.stock + :quantity where i.productId = :productId")
    int addStock(String productId, int quantity);
}
