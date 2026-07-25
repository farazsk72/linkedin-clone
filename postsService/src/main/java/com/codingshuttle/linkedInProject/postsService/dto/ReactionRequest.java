package com.codingshuttle.linkedInProject.postsService.dto;

import com.codingshuttle.linkedInProject.postsService.entity.ReactionType;
import lombok.Data;

/**
 * Body of a reaction request. The whole body - and the type within it - is
 * optional: an absent body or null type means a plain LIKE, which keeps the old
 * bodyless {@code POST /likes/{postId}} call working unchanged.
 */
@Data
public class ReactionRequest {
    private ReactionType type;
}
