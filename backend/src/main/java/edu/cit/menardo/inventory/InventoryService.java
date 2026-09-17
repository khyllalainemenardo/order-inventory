package edu.cit.menardo.inventory;

import java.util.List;
import java.util.Optional;

public interface InventoryService {

    List<InventoryView> listItems();

    Optional<InventoryView> getItem(String productId);

    ReservationResult reserve(String productId, int quantity);

    InventoryView restock(String productId, int quantity);
}
