package edu.cit.menardo.notification;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SuppressWarnings("unused")
class NotificationController {

    private final NotificationRepository repository;

    NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/notifications")
    List<NotificationView> notifications() {
        return repository.findTop100ByOrderByNotificationIdDesc().stream()
                .map(NotificationRecord::toView)
                .toList();
    }
}
