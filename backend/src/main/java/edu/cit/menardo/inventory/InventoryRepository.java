package edu.cit.menardo.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryRepository extends JpaRepository<InventoryItem, String> {

    /**
     * Conditional decrement. The "is there enough?" check and the write happen in
     * one statement, so two orders arriving at the same moment cannot both pass a
     * check and drive stock negative.
     *
     * @return 1 if stock was deducted, 0 if there was not enough
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update InventoryItem i
               set i.stock = i.stock - :quantity
             where i.productId = :productId
               and i.stock >= :quantity
            """)
    int deductIfAvailable(@Param("productId") String productId, @Param("quantity") int quantity);
}
