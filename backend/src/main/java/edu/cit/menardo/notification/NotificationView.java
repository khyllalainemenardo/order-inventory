package edu.cit.menardo.notification;

import java.time.OffsetDateTime;

/** One entry of GET /api/notifications. */
public record NotificationView(Long notificationId, String type, String message, OffsetDateTime createdAt) {
}
