package com.codingshuttle.linkedInProject.postsService.dto;

import lombok.Data;

@Data
public class CommentCreateRequestDto {
    private String content;

    /** Set to reply to an existing comment on the same post. */
    private Long parentCommentId;
}
