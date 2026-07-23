package com.codingshuttle.linkedInProject.notification_service.repository;

import com.codingshuttle.linkedInProject.notification_service.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    List<NotificationPreference> findByUserId(Long userId);

    Optional<NotificationPreference> findByUserIdAndType(Long userId, String type);

    boolean existsByUserIdAndTypeAndMutedTrue(Long userId, String type);
}
