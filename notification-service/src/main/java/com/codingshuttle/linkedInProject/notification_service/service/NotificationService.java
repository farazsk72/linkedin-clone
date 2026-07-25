package com.codingshuttle.linkedInProject.notification_service.service;

import com.codingshuttle.linkedInProject.notification_service.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.notification_service.dto.NotificationPreferenceDto;
import com.codingshuttle.linkedInProject.notification_service.entity.Notification;
import com.codingshuttle.linkedInProject.notification_service.entity.NotificationPreference;
import com.codingshuttle.linkedInProject.notification_service.exception.BadRequestException;
import com.codingshuttle.linkedInProject.notification_service.exception.ResourceNotFoundException;
import com.codingshuttle.linkedInProject.notification_service.repository.NotificationPreferenceRepository;
import com.codingshuttle.linkedInProject.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationPushService pushService;

    public List<Notification> getNotificationsOfCurrentUser() {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Getting notifications for user with ID: {}", userId);

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount() {
        Long userId = AuthContextHolder.getCurrentUserId();
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Marking notification with ID: {} as read for user with ID: {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: "+notificationId));

        // Reported as not-found rather than forbidden on purpose - confirming
        // that someone else's notification exists is itself a small leak.
        if(!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found with ID: "+notificationId);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead() {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Marking all notifications as read for user with ID: {}", userId);

        return notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Deleting notification with ID: {} for user with ID: {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: "+notificationId));

        // Not-found rather than forbidden, matching markAsRead - a 403 would
        // confirm someone else's notification exists.
        if(!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found with ID: "+notificationId);
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public int clearAll() {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Clearing all notifications for user with ID: {}", userId);

        return notificationRepository.deleteAllForUser(userId);
    }

    /** The types a user can mute. Anything not listed here is always delivered. */
    public static final List<String> MUTABLE_TYPES =
            List.of("POST_CREATED", "POST_LIKED", "POST_COMMENTED", "POST_REPOSTED", "USER_MENTIONED");

    public List<NotificationPreferenceDto> getPreferences() {
        Long userId = AuthContextHolder.getCurrentUserId();

        // Absence of a row means enabled, so the full list is built from the
        // known types rather than from whatever happens to be stored.
        Set<String> muted = preferenceRepository.findByUserId(userId).stream()
                .filter(NotificationPreference::isMuted)
                .map(NotificationPreference::getType)
                .collect(Collectors.toSet());

        return MUTABLE_TYPES.stream()
                .map((type) -> new NotificationPreferenceDto(type, !muted.contains(type)))
                .toList();
    }

    @Transactional
    public List<NotificationPreferenceDto> setPreference(String type, boolean enabled) {
        Long userId = AuthContextHolder.getCurrentUserId();

        if(type == null || !MUTABLE_TYPES.contains(type)) {
            throw new BadRequestException("Unknown notification type: "+type);
        }
        log.info("Setting notification type {} to enabled={} for user with ID: {}", type, enabled, userId);

        NotificationPreference preference = preferenceRepository.findByUserIdAndType(userId, type)
                .orElseGet(() -> {
                    NotificationPreference fresh = new NotificationPreference();
                    fresh.setUserId(userId);
                    fresh.setType(type);
                    return fresh;
                });

        preference.setMuted(!enabled);
        preferenceRepository.save(preference);

        return getPreferences();
    }

    public void addNotification(Notification notification) {
        // Dropped at the consumer rather than filtered on read, so a muted type
        // never accumulates unread rows the user has to clear later.
        if(notification.getType() != null
                && preferenceRepository.existsByUserIdAndTypeAndMutedTrue(
                        notification.getUserId(), notification.getType())) {
            log.info("Skipping muted {} notification for user with ID: {}",
                    notification.getType(), notification.getUserId());
            return;
        }

        log.info("Adding notification to db, message: {}", notification.getMessage());
        Notification saved = notificationRepository.save(notification);

        // Push it live so the recipient's navbar updates without waiting for the
        // next poll. Delivery is best-effort inside pushToUser.
        long unread = notificationRepository.countByUserIdAndReadFalse(saved.getUserId());
        pushService.pushToUser(saved, unread);

//        SendMailer to send email
//        FCM
    }
}
