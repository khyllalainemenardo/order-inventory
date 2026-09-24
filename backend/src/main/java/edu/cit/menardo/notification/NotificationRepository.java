package edu.cit.menardo.notification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationRepository extends JpaRepository<NotificationRecord, Long> {

    List<NotificationRecord> findTop100ByOrderByNotificationIdDesc();
}
