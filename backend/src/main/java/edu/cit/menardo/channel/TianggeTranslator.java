package edu.cit.menardo.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import edu.cit.menardo.channel.TianggeJson.FeedEvent;
import edu.cit.menardo.channel.TianggeJson.Line;
import edu.cit.menardo.channel.TianggeJson.Listing;
import edu.cit.menardo.channel.TianggeJson.StockEntry;
import edu.cit.menardo.inventory.InventoryView;
import edu.cit.menardo.shop.OrderRequest;
import org.springframework.stereotype.Component;

@Component
class TianggeTranslator {

    static final String ORDER_PLACED = "ORDER_PLACED";
    static final String ORDER_CANCELLED = "ORDER_CANCELLED";

    private static final int MAX_REASON = 200;

    Listing toListing(InventoryView item, String supplierItem) {
        return new Listing(item.productId(), item.name(), supplierItem);
    }

    StockEntry toStock(InventoryView item) {
        return new StockEntry(item.productId(), Math.max(0, item.stock()));
    }

    Optional<OrderRequest> toOrderRequest(FeedEvent event, Set<String> listedSkus) {
        if (event.lines() == null || event.lines().isEmpty()) {
            return Optional.empty();
        }
        List<OrderRequest.Item> items = new ArrayList<>();
        for (Line line : event.lines()) {
            if (line == null || line.sellerSku() == null || !listedSkus.contains(line.sellerSku())
                    || line.qty() == null || line.qty() < 1) {
                return Optional.empty();
            }
            items.add(new OrderRequest.Item(line.sellerSku(), line.qty()));
        }
        return Optional.of(new OrderRequest(items));
    }

    ChannelDecision toDecision(String shopOrderStatus) {
        return switch (shopOrderStatus) {
            case "CONFIRMED" -> ChannelDecision.ACCEPTED;
            case "BACKORDERED" -> ChannelDecision.BACKORDERED;
            default -> ChannelDecision.REJECTED;
        };
    }

    String toReason(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() <= MAX_REASON ? reason : reason.substring(0, MAX_REASON - 3) + "...";
    }
}
