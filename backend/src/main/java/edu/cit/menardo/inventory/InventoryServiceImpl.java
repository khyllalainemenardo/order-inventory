package edu.cit.menardo.inventory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import edu.cit.menardo.inventory.events.LowStockEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Package-private implementation - note there is no 'public' modifier on the class.
 * Spring can still create and inject it; the other modules cannot name the type.
 */
@Service
class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repository;
    private final ApplicationEventPublisher events;
    private final int lowStockThreshold;

    InventoryServiceImpl(InventoryRepository repository,
                         ApplicationEventPublisher events,
                         @Value("${inventory.low-stock-threshold:5}") int lowStockThreshold) {
        this.repository = repository;
        this.events = events;
        this.lowStockThreshold = lowStockThreshold;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryView> listItems() {
        return repository.findAll().stream()
                .map(this::view)
                .sorted(Comparator.comparing(InventoryView::productId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryView> getItem(String productId) {
        return repository.findById(productId).map(this::view);
    }

    @Override
    @Transactional
    public ReservationResult reserve(String productId, int quantity) {
        if (quantity < 1) {
            return ReservationResult.rejected("Quantity must be at least 1.", null);
        }

        if (repository.findById(productId).isEmpty()) {
            return ReservationResult.rejected("No product with id " + productId + ".", null);
        }

        int rowsChanged = repository.deductIfAvailable(productId, quantity);
        InventoryView current = repository.findById(productId).map(this::view).orElseThrow();

        if (rowsChanged == 0) {
            String reason = "Only %d of %s left, %d requested."
                    .formatted(current.stock(), current.name(), quantity);
            return ReservationResult.rejected(reason, current);
        }

        // Low-stock rule: every successful reservation that leaves the product
        // below the threshold raises an alert. Inventory owns the stock and the
        // threshold, so the rule lives here rather than in the Order module.
        if (current.lowStock()) {
            events.publishEvent(new LowStockEvent(
                    current.productId(), current.name(), current.stock(), lowStockThreshold));
        }

        return ReservationResult.confirmed(current);
    }

    @Override
    @Transactional
    public InventoryView restock(String productId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Restock quantity must be at least 1.");
        }
        if (repository.addStock(productId, quantity) == 0) {
            throw new IllegalArgumentException("No product with id " + productId + ".");
        }
        return repository.findById(productId).map(this::view).orElseThrow();
    }

    private InventoryView view(InventoryItem item) {
        return item.toView(lowStockThreshold);
    }
}
