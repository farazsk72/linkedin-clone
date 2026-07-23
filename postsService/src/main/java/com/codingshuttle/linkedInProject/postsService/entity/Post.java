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
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    private String imageUrl;

    @Column(nullable = false)
    private Long userId;

    /**
     * PUBLIC or CONNECTIONS. Stored as text with a column default so that
     * ddl-auto=update can add it to a populated table - existing posts become
     * PUBLIC, which is how they already behaved.
     */
    @Column(nullable = false, columnDefinition = "varchar(20) default 'PUBLIC'")
    private String visibility = "PUBLIC";

    /** Set when this post is a repost; null otherwise. */
    private Long originalPostId;

    /**
     * PUBLISHED or DRAFT. Same column-default trick as visibility, so existing
     * rows become PUBLISHED - which is what they already were.
     */
    @Column(nullable = false, columnDefinition = "varchar(20) default 'PUBLISHED'")
    private String status = "PUBLISHED";

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Nullable on purpose - rows written before this column existed were never
    // edited, and null is exactly what "never edited" should look like.
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
