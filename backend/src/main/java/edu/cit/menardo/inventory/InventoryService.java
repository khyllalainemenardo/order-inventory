package edu.cit.menardo.inventory;

import java.util.List;
import java.util.Optional;

/**
 * The published boundary of the Inventory module.
 *
 * This interface, InventoryView, ReservationResult and the events package are
 * the only public types. The entity, the repository and the implementation are
 * package-private, so the other modules physically cannot compile against them.
 */
public interface InventoryService {

    /** Every stocked product, for the inventory table and the cart picker. */
    List<InventoryView> listItems();

    /** A single product, empty if the id is unknown. */
    Optional<InventoryView> getItem(String productId);

    /**
     * Deducts {@code quantity} from stock if there is enough of it.
     * Never throws for the ordinary "not enough stock" case - that is a
     * business outcome, so it comes back as a rejected ReservationResult.
     * Publishes a LowStockEvent when the remaining stock ends up below the threshold.
     */
    ReservationResult reserve(String productId, int quantity);

    /**
     * Puts {@code quantity} back on the shelf, used when an order is cancelled.
     *
     * @throws IllegalArgumentException if the product is unknown or quantity is below 1
     */
    InventoryView restock(String productId, int quantity);
}
