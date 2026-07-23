package com.codingshuttle.linkedInProject.postsService.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostCommented {
    private Long postId;
    private Long ownerUserId;
    private Long commentedByUserId;
    private String content;
}
