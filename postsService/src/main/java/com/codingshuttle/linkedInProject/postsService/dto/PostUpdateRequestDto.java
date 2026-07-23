package com.codingshuttle.linkedInProject.postsService.dto;

import lombok.Data;

@Data
public class PostUpdateRequestDto {
    private String content;

    /**
     * Replacing an image means re-uploading, which this JSON endpoint cannot
     * do. Dropping one is the case worth supporting, so it gets a flag rather
     * than an overloaded null.
     */
    private boolean removeImage;
}
