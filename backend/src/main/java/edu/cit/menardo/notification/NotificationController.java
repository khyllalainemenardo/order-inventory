package edu.cit.menardo.notification;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/notifications - the activity feed, newest first. */
@RestController
@RequestMapping("/api")
class NotificationController {

    private final NotificationRepository repository;

    NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/notifications")
    List<NotificationView> notifications() {
        return repository.findTop100ByOrderByCreatedAtDescNotificationIdDesc().stream()
                .map(NotificationRecord::toView)
                .toList();
    }
}
