package com.codingshuttle.linkedInProject.notification_service.repository;

import com.codingshuttle.linkedInProject.notification_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    // clearAutomatically, otherwise entities already loaded in the persistence
    // context would keep reporting the stale read flag.
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true where n.userId = :userId and n.read = false")
    int markAllAsRead(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Notification n where n.userId = :userId")
    int deleteAllForUser(@Param("userId") Long userId);
}
