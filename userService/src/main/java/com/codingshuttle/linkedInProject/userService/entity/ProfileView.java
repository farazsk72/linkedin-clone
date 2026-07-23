package com.codingshuttle.linkedInProject.userService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One row per viewer per profile, updated in place rather than appended - the
 * feature answers "who looked at me and when", so an unbounded append log would
 * grow fast and still only ever show the latest visit per person.
 */
@Entity
@Getter
@Setter
@Table(
        name = "profile_views",
        uniqueConstraints = @UniqueConstraint(columnNames = {"profileUserId", "viewerUserId"})
)
public class ProfileView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long profileUserId;

    @Column(nullable = false)
    private Long viewerUserId;

    @CreationTimestamp
    private LocalDateTime firstViewedAt;

    @Column(nullable = false)
    private LocalDateTime lastViewedAt;
}
