package edu.cit.menardo.channel;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

final class TianggeJson {

    private TianggeJson() {
    }

    record Heartbeat(String appName, String startedAt, long uptimeSeconds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HeartbeatReply(String serverTime, Integer nextHeartbeatSeconds) {
    }

    record Listing(String sellerSku, String title, String supplierSku) {
    }

    record StockEntry(String sellerSku, int available) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FeedPage(List<FeedEvent> events, Long nextCursor) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FeedEvent(long seq, String eventId, String type, String orderId,
                     String placedAt, String decisionDeadline,
                     String cancelledAt, String confirmDeadline,
                     List<Line> lines) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Line(String sellerSku, Integer qty) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Decision(String decision, String shopOrderId, String reason) {
    }

    record Resolution(String status) {
    }

    record CancellationConfirmation(boolean restocked) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Error(String error, String message) {
    }
}
