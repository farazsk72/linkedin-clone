package com.codingshuttle.linkedInProject.postsService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long userId;

    /**
     * Null for a top-level comment. Threading is one level deep on purpose -
     * replying to a reply attaches to the same parent, so a thread cannot
     * become an unbounded tree.
     */
    private Long parentCommentId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Equal to createdAt until the comment is edited - same shape as Post. */
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
