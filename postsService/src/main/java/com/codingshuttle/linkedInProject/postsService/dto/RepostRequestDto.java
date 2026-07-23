package com.codingshuttle.linkedInProject.postsService.dto;

import lombok.Data;

@Data
public class RepostRequestDto {
    /** Optional commentary. A bare repost carries none. */
    private String content;

    private String visibility;
}
