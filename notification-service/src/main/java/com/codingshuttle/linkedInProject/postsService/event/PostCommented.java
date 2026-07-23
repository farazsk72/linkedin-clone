package com.codingshuttle.linkedInProject.postsService.event;

import lombok.Data;

/**
 * Consumer-side copy. @Data only, with no @Builder - @Builder generates an
 * all-args constructor which suppresses the no-arg one Jackson needs to
 * instantiate the event.
 */
@Data
public class PostCommented {
    private Long postId;
    private Long ownerUserId;
    private Long commentedByUserId;
    private String content;
}
