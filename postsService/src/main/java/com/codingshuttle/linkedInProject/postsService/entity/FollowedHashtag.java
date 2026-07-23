package com.codingshuttle.linkedInProject.postsService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
        name = "followed_hashtags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "tag"})
)
public class FollowedHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** Stored lower-cased and without '#', to match how PostHashtag stores tags. */
    @Column(nullable = false)
    private String tag;
}
