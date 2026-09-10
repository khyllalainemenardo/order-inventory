package edu.cit.menardo.inventory;

import java.util.List;
import java.util.Optional;

/**
 * The published boundary of the Inventory module.
 *
 * This interface, InventoryView and ReservationResult are the only public types
 * in the package. Everything else - the entity, the repository, the
 * implementation - is package-private, so the Order module physically cannot
 * compile against them.
 */
public interface InventoryService {

    /** Every stocked product, for the front-end dropdown. */
    List<InventoryView> listItems();

    /** A single product, empty if the id is unknown. */
    Optional<InventoryView> getItem(String productId);

    /**
     * Deducts {@code quantity} from stock if there is enough of it.
     * Never throws for the ordinary "not enough stock" case - that is a
     * business outcome, so it comes back as a rejected ReservationResult.
     */
    ReservationResult reserve(String productId, int quantity);
}
