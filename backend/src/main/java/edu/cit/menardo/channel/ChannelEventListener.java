package edu.cit.menardo.channel;

import java.util.UUID;

import edu.cit.menardo.inventory.events.StockChangedEvent;
import edu.cit.menardo.shop.events.BackorderCancelledEvent;
import edu.cit.menardo.shop.events.BackorderFilledEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class ChannelEventListener {

    private final TianggeOutbox outbox;
    private final ChannelOrderRepository orders;
    private final TransactionTemplate newTransaction;

    ChannelEventListener(TianggeOutbox outbox, ChannelOrderRepository orders, PlatformTransactionManager transactions) {
        this.outbox = outbox;
        this.orders = orders;
        this.newTransaction = new TransactionTemplate(transactions);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(StockChangedEvent event) {
        outbox.stockChanged(event.productId());
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(BackorderFilledEvent event) {
        resolve(event.orderId(), ChannelDecision.ACCEPTED);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(BackorderCancelledEvent event) {
        resolve(event.orderId(), ChannelDecision.CANCELLED);
    }

    private void resolve(UUID shopOrderId, ChannelDecision resolution) {
        String tianggeOrderId = newTransaction.execute(status -> orders.findByShopOrderId(shopOrderId)
                .map(order -> {
                    order.resolve(resolution);
                    orders.save(order);
                    return order.getTianggeOrderId();
                })
                .orElse(null));
        if (tianggeOrderId != null) {
            outbox.resolutionReady(tianggeOrderId);
        }
    }
}
