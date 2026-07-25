package com.codingshuttle.linkedInProject.postsService.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostLiked {
    private Long postId;
    private Long ownerUserId;
    private Long likedByUserId;

    // Which reaction was left. Nullable for backward compatibility: events
    // serialized before reactions existed have no type, and the consumer treats
    // an absent value as a plain LIKE.
    private String reactionType;
}
