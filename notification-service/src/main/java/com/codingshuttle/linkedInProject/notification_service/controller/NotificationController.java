package com.codingshuttle.linkedInProject.notification_service.controller;

import com.codingshuttle.linkedInProject.notification_service.dto.NotificationPreferenceDto;
import com.codingshuttle.linkedInProject.notification_service.entity.Notification;
import com.codingshuttle.linkedInProject.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/core")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications() {
        return ResponseEntity.ok(notificationService.getNotificationsOfCurrentUser());
    }

    /** Cheap enough for the navbar to poll - a COUNT, not the whole list. */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount());
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<List<NotificationPreferenceDto>> getPreferences() {
        return ResponseEntity.ok(notificationService.getPreferences());
    }

    /** Muting takes effect at delivery, so muted events never reach the list. */
    @PutMapping("/preferences")
    public ResponseEntity<List<NotificationPreferenceDto>> setPreference(
            @RequestBody NotificationPreferenceDto dto) {
        return ResponseEntity.ok(notificationService.setPreference(dto.getType(), dto.isEnabled()));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }

    /** Clears the caller's whole list. Idempotent - clearing an empty one is fine. */
    @DeleteMapping
    public ResponseEntity<Void> clearAll() {
        notificationService.clearAll();
        return ResponseEntity.noContent().build();
    }
}
