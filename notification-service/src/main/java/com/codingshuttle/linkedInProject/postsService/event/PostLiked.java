package com.codingshuttle.linkedInProject.postsService.event;

import lombok.Data;

@Data
public class PostLiked {
    private Long postId;
    private Long ownerUserId;
    private Long likedByUserId;

    // Absent on events serialized before reactions existed; the consumer treats
    // null as a plain LIKE.
    private String reactionType;
}
