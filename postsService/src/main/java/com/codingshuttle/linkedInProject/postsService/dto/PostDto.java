package com.codingshuttle.linkedInProject.postsService.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PostDto {
    private Long id;
    private String content;
    private String imageUrl;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Total reactions of every type, kept for backward compatibility. likedByMe
    // is now "has any reaction", true for a LIKE or any other type.
    private Long likeCount;
    private Boolean likedByMe;
    // Per-type breakdown (e.g. {"LIKE":3,"CELEBRATE":1}) and the caller's own
    // reaction, or null if they have not reacted.
    private Map<String, Long> reactionCounts;
    private String myReaction;
    private Long commentCount;
    private Boolean savedByMe;
    private String visibility;
    private String status;
    private List<String> hashtags;

    /**
     * The post this one reposts, or null. Only ever one level deep - reposting
     * a repost points at the underlying original, so this never nests.
     */
    private PostDto originalPost;
}
