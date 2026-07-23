package com.codingshuttle.linkedInProject.postsService.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDto {
    private Long id;
    private String content;
    private String imageUrl;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long likeCount;
    private Boolean likedByMe;
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
