package com.codingshuttle.linkedInProject.postsService.event;

import lombok.Data;

/**
 * Consumer-side copy. @Data only, no @Builder - @Builder generates an all-args
 * constructor which suppresses the no-arg one Jackson needs.
 */
@Data
public class PostReposted {
    private Long postId;
    private Long ownerUserId;
    private Long repostedByUserId;
}
