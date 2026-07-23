package com.codingshuttle.linkedInProject.postsService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "post_hashtags", indexes = @Index(name = "idx_post_hashtag_tag", columnList = "tag"))
public class PostHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    /** Stored lower-cased and without the leading '#', so lookups are exact. */
    @Column(nullable = false)
    private String tag;
}
