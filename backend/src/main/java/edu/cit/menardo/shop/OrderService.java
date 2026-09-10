package edu.cit.menardo.shop;

import edu.cit.menardo.inventory.InventoryService;
import edu.cit.menardo.inventory.InventoryView;
import edu.cit.menardo.inventory.ReservationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration point between the two modules.
 *
 * The only thing this class knows about Inventory is the InventoryService
 * interface, injected through the constructor. No HTTP, no queue, no shared
 * entity - a plain method call inside one JVM and one transaction.
 */
@Service
public class OrderService {

    static final String CONFIRMED = "CONFIRMED";
    static final String REJECTED = "REJECTED";

    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;

    OrderService(InventoryService inventoryService, OrderRepository orderRepository) {
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
    }

    /**
     * One transaction covers both the stock deduction and the order row. If the
     * insert fails, the reservation rolls back with it and stock is never lost.
     * Rejected attempts are recorded too, so the orders table is a full audit trail.
     */
    @Transactional
    public OrderResponse place(OrderRequest request) {
        String productId = request.productId() == null ? "" : request.productId().trim();
        int quantity = request.quantity() == null ? 0 : request.quantity();

        if (productId.isEmpty()) {
            return record(REJECTED, "Choose a product before ordering.", null, productId, quantity);
        }

        ReservationResult reservation = inventoryService.reserve(productId, quantity);

        if (!reservation.confirmed()) {
            return record(REJECTED, reservation.reason(), reservation.inventory(), productId, quantity);
        }

        return record(CONFIRMED, null, reservation.inventory(), productId, quantity);
    }

    private OrderResponse record(String status,
                                 String reason,
                                 InventoryView inventory,
                                 String productId,
                                 int quantity) {
        orderRepository.save(new OrderRecord(productId, quantity, status, reason));
        return new OrderResponse(status, reason, inventory);
    }
}
