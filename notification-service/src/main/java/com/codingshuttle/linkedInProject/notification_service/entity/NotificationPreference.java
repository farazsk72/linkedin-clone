package com.codingshuttle.linkedInProject.notification_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A row exists only for a type the user has explicitly muted. Absence means
 * "enabled", so nothing has to be seeded for existing users and a new event
 * type is on by default.
 */
@Entity
@Getter
@Setter
@Table(
        name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "type"})
)
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private boolean muted = true;
}
