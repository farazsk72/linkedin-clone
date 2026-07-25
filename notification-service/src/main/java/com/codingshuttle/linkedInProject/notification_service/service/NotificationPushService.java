package com.codingshuttle.linkedInProject.notification_service.service;

import com.codingshuttle.linkedInProject.notification_service.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Pushes a freshly saved notification to its owner over WebSocket. Delivery is
 * best-effort: if the user has no live session the simple broker just drops the
 * message, and any push failure is swallowed so it can never break the Kafka
 * consumer that persisted the notification. The REST list plus the navbar poll
 * remain the source of truth - this only removes the polling latency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPushService {

    static final String NEW_NOTIFICATION_DESTINATION = "/queue/notifications";
    static final String UNREAD_COUNT_DESTINATION = "/queue/notifications.unread";

    private final SimpMessagingTemplate messagingTemplate;

    public void pushToUser(Notification notification, long unreadCount) {
        String userId = String.valueOf(notification.getUserId());
        try {
            messagingTemplate.convertAndSendToUser(userId, NEW_NOTIFICATION_DESTINATION, notification);
            messagingTemplate.convertAndSendToUser(userId, UNREAD_COUNT_DESTINATION, unreadCount);
        } catch (RuntimeException e) {
            // The notification is already persisted; a delivery hiccup must not
            // fail the consumer or the user will never see it via the poll either.
            log.warn("Failed to push live notification to user {}: {}", userId, e.getMessage());
        }
    }
}
