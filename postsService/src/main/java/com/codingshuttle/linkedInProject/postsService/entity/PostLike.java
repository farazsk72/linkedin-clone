package com.codingshuttle.linkedInProject.postsService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@Table(name = "post_likes")
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long postId;

    // The reaction kind. The column default backfills rows that predate
    // reactions (all LIKEs), since ddl-auto=update cannot add a NOT NULL column
    // to a populated table without one.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'LIKE'")
    private ReactionType type = ReactionType.LIKE;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
