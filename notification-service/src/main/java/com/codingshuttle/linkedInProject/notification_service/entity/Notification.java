package com.codingshuttle.linkedInProject.notification_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String message;

    // Structured target, so the UI can link to what the notification is about
    // instead of the user having to read an id out of the message text. Both
    // nullable - rows written before these existed have neither.
    private String type;
    private Long targetId;

    // "read" is mapped to is_read - the column default is what backfills rows
    // that existed before this field did, since ddl-auto=update cannot add a
    // NOT NULL column to a populated table without one.
    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean read = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
