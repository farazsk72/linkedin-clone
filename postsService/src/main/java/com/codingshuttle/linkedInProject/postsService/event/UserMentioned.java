package com.codingshuttle.linkedInProject.postsService.event;

import lombok.Builder;
import lombok.Data;

/**
 * A user was @mentioned in a post or a comment. One event per mentioned user;
 * the mentioner is never a recipient of their own mention.
 */
@Data
@Builder
public class UserMentioned {
    private Long mentionedUserId;   // who to notify
    private Long mentionedByUserId; // who wrote the mention
    private Long postId;            // the post to link the notification to
    private String context;         // "POST" or "COMMENT"
}
