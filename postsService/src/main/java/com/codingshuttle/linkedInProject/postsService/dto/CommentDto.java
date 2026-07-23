package com.codingshuttle.linkedInProject.postsService.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentDto {
    private Long id;
    private String content;
    private Long postId;
    private Long userId;
    private Long parentCommentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long likeCount;
    private boolean likedByMe;

    /** Populated only on top-level comments; always empty on a reply. */
    private List<CommentDto> replies;
}
