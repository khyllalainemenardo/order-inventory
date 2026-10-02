package edu.cit.menardo.inventory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import edu.cit.menardo.inventory.events.LowStockEvent;
import edu.cit.menardo.inventory.events.StockChangedEvent;
import edu.cit.menardo.supplier.SupplierGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repository;
    private final ApplicationEventPublisher events;
    private final SupplierGateway supplierGateway;
    private final int lowStockThreshold;
    private final int reorderTarget;

    InventoryServiceImpl(InventoryRepository repository,
                         ApplicationEventPublisher events,
                         SupplierGateway supplierGateway,
                         @Value("${inventory.low-stock-threshold}") int lowStockThreshold,
                         @Value("${inventory.reorder-target}") int reorderTarget) {
        this.repository = repository;
        this.events = events;
        this.supplierGateway = supplierGateway;
        this.lowStockThreshold = lowStockThreshold;
        this.reorderTarget = reorderTarget;
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
        events.publishEvent(new StockChangedEvent(productId, -quantity, item.stock()));

        if (item.lowStock()) {
            events.publishEvent(new LowStockEvent(item.productId(), item.name(), item.stock(), lowStockThreshold));
            supplierGateway.requestReorder(item.productId(), reorderTarget - item.stock());
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
        InventoryView item = getItem(productId).orElseThrow();
        events.publishEvent(new StockChangedEvent(productId, quantity, item.stock()));
        return item;
    }

    @Override
    public int incomingUnits(String productId) {
        return supplierGateway.unitsOnTheWay(productId);
    }

    @Override
    @Transactional
    public void reportShortage(String productId, int quantityWanted) {
        InventoryView item = getItem(productId).orElse(null);
        if (item == null) {
            return;
        }
        int unitsNeeded = Math.max(reorderTarget - item.stock(), quantityWanted - item.stock());
        supplierGateway.requestReorder(productId, Math.max(1, unitsNeeded));
    }
}
