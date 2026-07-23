package com.codingshuttle.linkedInProject.postsService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
        name = "comment_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "commentId"})
)
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long commentId;
}
