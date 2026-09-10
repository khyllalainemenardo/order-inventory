package edu.cit.menardo.inventory;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Package-private implementation - note there is no 'public' modifier on the class.
 * Spring can still create and inject it; the Order module cannot name the type.
 */
@Service
class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repository;

    InventoryServiceImpl(InventoryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryView> listItems() {
        return repository.findAll().stream()
                .map(InventoryItem::toView)
                .sorted((a, b) -> a.productId().compareTo(b.productId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryView> getItem(String productId) {
        return repository.findById(productId).map(InventoryItem::toView);
    }

    @Override
    @Transactional
    public ReservationResult reserve(String productId, int quantity) {
        if (quantity < 1) {
            return ReservationResult.rejected("Quantity must be at least 1.", null);
        }

        Optional<InventoryItem> found = repository.findById(productId);
        if (found.isEmpty()) {
            return ReservationResult.rejected("No product with id " + productId + ".", null);
        }

        int rowsChanged = repository.deductIfAvailable(productId, quantity);
        InventoryView current = repository.findById(productId)
                .map(InventoryItem::toView)
                .orElseThrow();

        if (rowsChanged == 0) {
            String reason = "Only %d of %s left, %d requested."
                    .formatted(current.stock(), current.name(), quantity);
            return ReservationResult.rejected(reason, current);
        }

        return ReservationResult.confirmed(current);
    }
}
