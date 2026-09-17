package edu.cit.menardo.notification;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Maps the notifications table. Package-private: only this module writes to it. */
@Entity
@Table(name = "notifications")
class NotificationRecord {

    static final String ORDER_CONFIRMED = "ORDER_CONFIRMED";
    static final String ORDER_REJECTED = "ORDER_REJECTED";
    static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    static final String LOW_STOCK = "LOW_STOCK";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false, updatable = false)
    private Long notificationId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected NotificationRecord() {
        // required by JPA
    }

    NotificationRecord(String type, String message) {
        this.type = type;
        this.message = message;
        this.createdAt = OffsetDateTime.now();
    }

    String getType() {
        return type;
    }

    String getMessage() {
        return message;
    }

    NotificationView toView() {
        return new NotificationView(notificationId, type, message, createdAt);
    }
}
