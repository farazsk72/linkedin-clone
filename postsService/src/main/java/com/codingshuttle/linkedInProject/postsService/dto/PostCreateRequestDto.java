package com.codingshuttle.linkedInProject.postsService.dto;

import lombok.Data;

@Data
public class PostCreateRequestDto {
    private String content;

    /** PUBLIC (default) or CONNECTIONS. */
    private String visibility;

    /** True to save without publishing. Drafts notify nobody and appear nowhere. */
    private boolean draft;
}
