package edu.cit.menardo.notification;

import java.time.OffsetDateTime;

public record NotificationView(Long notificationId, String type, String message, OffsetDateTime createdAt) {
}
