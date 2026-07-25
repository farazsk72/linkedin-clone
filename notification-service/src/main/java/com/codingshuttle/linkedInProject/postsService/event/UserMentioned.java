package com.codingshuttle.linkedInProject.postsService.event;

import lombok.Data;

@Data
public class UserMentioned {
    private Long mentionedUserId;
    private Long mentionedByUserId;
    private Long postId;
    private String context;
}
