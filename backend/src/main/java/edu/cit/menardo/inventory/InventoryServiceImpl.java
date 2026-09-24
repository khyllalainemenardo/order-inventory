package edu.cit.menardo.inventory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import edu.cit.menardo.inventory.events.LowStockEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repository;
    private final ApplicationEventPublisher events;
    private final int lowStockThreshold;

    InventoryServiceImpl(InventoryRepository repository,
                         ApplicationEventPublisher events,
                         @Value("${inventory.low-stock-threshold}") int lowStockThreshold) {
        this.repository = repository;
        this.events = events;
        this.lowStockThreshold = lowStockThreshold;
    }

    @Override
    public List<InventoryView> listItems() {
        return repository.findAll().stream()
                .map(item -> item.toView(lowStockThreshold))
                .sorted(Comparator.comparing(InventoryView::productId))
                .toList();
    }

    @Override
    public Optional<InventoryView> getItem(String productId) {
        return repository.findById(productId).map(item -> item.toView(lowStockThreshold));
    }

    @Override
    @Transactional
    public ReservationResult reserve(String productId, int quantity) {
        boolean deducted = repository.deductIfAvailable(productId, quantity) == 1;
        InventoryView item = getItem(productId).orElse(null);

        if (!deducted) {
            return ReservationResult.rejected("Not enough stock for " + productId + ".", item);
        }

        if (item.lowStock()) {
            events.publishEvent(new LowStockEvent(item.productId(), item.name(), item.stock(), lowStockThreshold));
        }
        return ReservationResult.confirmed(item);
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
        return getItem(productId).orElseThrow();
    }
}
