package edu.cit.menardo.channel;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "channel_orders")
class ChannelOrder {

    @Id
    @Column(name = "tiangge_order_id")
    private String tianggeOrderId;

    @Column(name = "shop_order_id")
    private UUID shopOrderId;

    private String decision;

    private String reason;

    @Column(name = "decision_sent")
    private boolean decisionSent;

    private String resolution;

    @Column(name = "resolution_sent")
    private boolean resolutionSent;

    @Column(name = "cancel_received")
    private boolean cancelReceived;

    @Column(name = "cancel_confirmed")
    private boolean cancelConfirmed;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected ChannelOrder() {
    }

    private ChannelOrder(String tianggeOrderId, UUID shopOrderId, String decision, String reason, boolean decisionSent) {
        this.tianggeOrderId = tianggeOrderId;
        this.shopOrderId = shopOrderId;
        this.decision = decision;
        this.reason = reason;
        this.decisionSent = decisionSent;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    static ChannelOrder decided(String tianggeOrderId, UUID shopOrderId, ChannelDecision decision, String reason) {
        return new ChannelOrder(tianggeOrderId, shopOrderId, decision.name(), reason, false);
    }

    static ChannelOrder unknownCancelled(String tianggeOrderId) {
        ChannelOrder order = new ChannelOrder(tianggeOrderId, null, null, null, true);
        order.cancelReceived();
        return order;
    }

    void decisionSent() {
        this.decisionSent = true;
        touch();
    }

    void resolve(ChannelDecision resolution) {
        if (this.resolution == null) {
            this.resolution = resolution.name();
            touch();
        }
    }

    void resolutionSent() {
        this.resolutionSent = true;
        touch();
    }

    void cancelReceived() {
        this.cancelReceived = true;
        touch();
    }

    void cancelConfirmed() {
        this.cancelConfirmed = true;
        touch();
    }

    private void touch() {
        this.updatedAt = OffsetDateTime.now();
    }

    String getTianggeOrderId() {
        return tianggeOrderId;
    }

    UUID getShopOrderId() {
        return shopOrderId;
    }

    ChannelDecision getDecision() {
        return decision == null ? null : ChannelDecision.valueOf(decision);
    }

    String getReason() {
        return reason;
    }

    boolean isDecisionSent() {
        return decisionSent;
    }

    ChannelDecision getResolution() {
        return resolution == null ? null : ChannelDecision.valueOf(resolution);
    }

    boolean isResolutionSent() {
        return resolutionSent;
    }

    boolean isCancelReceived() {
        return cancelReceived;
    }

    boolean isCancelConfirmed() {
        return cancelConfirmed;
    }
}
