package edu.cit.menardo.notification;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    private String type;

    private String message;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected NotificationRecord() {
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
