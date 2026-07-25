package com.codingshuttle.linkedInProject.notification_service.service;

import com.codingshuttle.linkedInProject.notification_service.entity.Notification;
import com.codingshuttle.linkedInProject.notification_service.repository.NotificationPreferenceRepository;
import com.codingshuttle.linkedInProject.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The consumer's delivery path: an unmuted notification is saved and pushed
 * live, a muted one is dropped before either happens (so a muted type never
 * accumulates unread rows the user has to clear).
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationPreferenceRepository preferenceRepository;
    @Mock NotificationPushService pushService;

    @InjectMocks NotificationService notificationService;

    private Notification likeNotification() {
        return Notification.builder()
                .userId(9L)
                .type("POST_LIKED")
                .message("User 3 liked your post")
                .build();
    }

    @Test
    void savesAndPushesAnUnmutedNotification() {
        Notification notification = likeNotification();
        when(preferenceRepository.existsByUserIdAndTypeAndMutedTrue(9L, "POST_LIKED"))
                .thenReturn(false);
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationRepository.countByUserIdAndReadFalse(9L)).thenReturn(3L);

        notificationService.addNotification(notification);

        verify(notificationRepository).save(notification);
        verify(pushService).pushToUser(notification, 3L);
    }

    @Test
    void dropsAndDoesNotPushAMutedNotification() {
        Notification notification = likeNotification();
        when(preferenceRepository.existsByUserIdAndTypeAndMutedTrue(9L, "POST_LIKED"))
                .thenReturn(true);

        notificationService.addNotification(notification);

        verify(notificationRepository, never()).save(any());
        verify(pushService, never()).pushToUser(any(), anyLong());
    }
}
