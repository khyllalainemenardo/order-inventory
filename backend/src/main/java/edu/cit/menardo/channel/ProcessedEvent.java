package edu.cit.menardo.channel;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "channel_events")
class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;

    private long seq;

    private String type;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    protected ProcessedEvent() {
    }

    ProcessedEvent(String eventId, long seq, String type, String orderId) {
        this.eventId = eventId;
        this.seq = seq;
        this.type = type;
        this.orderId = orderId;
        this.processedAt = OffsetDateTime.now();
    }
}
