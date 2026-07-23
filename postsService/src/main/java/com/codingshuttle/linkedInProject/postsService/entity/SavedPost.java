package com.codingshuttle.linkedInProject.postsService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(
        name = "saved_posts",
        // Saving twice is a no-op rather than a second row - the service checks
        // first, and this stops a race from getting past it.
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "postId"})
)
public class SavedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long postId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
