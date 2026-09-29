package edu.cit.menardo.supplier;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReorderService implements SupplierGateway {

    private static final Logger log = LoggerFactory.getLogger(ReorderService.class);

    private final SupplierOrderRepository repository;
    private final LegacySupplyTranslator translator;
    private final ApplicationEventPublisher events;

    ReorderService(SupplierOrderRepository repository,
                   LegacySupplyTranslator translator,
                   ApplicationEventPublisher events) {
        this.repository = repository;
        this.translator = translator;
        this.events = events;
    }

    @Override
    @Transactional
    public ReorderResult requestReorder(String productId, int unitsNeeded) {
        Optional<SupplierOrder> openOrder =
                repository.findFirstByProductIdAndStatusIn(productId, SupplierOrderStatus.OPEN);
        if (openOrder.isPresent()) {
            return openOrder.get().toResult();
        }

        if (!translator.knows(productId)) {
            log.warn("No supplier item for product {}, reorder skipped", productId);
            return new ReorderResult(null, productId, 0, SupplierOrderStatus.FAILED);
        }

        int cases = translator.casesFor(productId, unitsNeeded);
        int units = translator.unitsIn(productId, cases);
        SupplierOrder order = repository.save(new SupplierOrder(productId, cases, units));
        order.assignBuyerRef();

        events.publishEvent(new ReorderSaved(order.getId()));
        return order.toResult();
    }
}
