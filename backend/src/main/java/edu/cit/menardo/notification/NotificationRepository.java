package edu.cit.menardo.notification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationRepository extends JpaRepository<NotificationRecord, Long> {

    /** The activity feed, newest first. Id breaks ties inside one transaction. */
    List<NotificationRecord> findTop100ByOrderByCreatedAtDescNotificationIdDesc();
}
