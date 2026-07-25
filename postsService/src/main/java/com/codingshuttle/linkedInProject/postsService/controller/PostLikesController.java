package com.codingshuttle.linkedInProject.postsService.controller;

import com.codingshuttle.linkedInProject.postsService.dto.ReactionRequest;
import com.codingshuttle.linkedInProject.postsService.entity.ReactionType;
import com.codingshuttle.linkedInProject.postsService.service.PostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/likes")
@RequiredArgsConstructor
@RestController
public class PostLikesController {

    private final PostLikeService postLikeService;

    /**
     * Sets the caller's reaction. The body is optional: no body (or a null type)
     * means a plain LIKE, so the original bodyless call still works. Any
     * {@link ReactionType} may be sent to react differently or to change an
     * existing reaction.
     */
    @PostMapping("/{postId}")
    public ResponseEntity<Void> react(@PathVariable Long postId,
                                      @RequestBody(required = false) ReactionRequest request) {
        ReactionType type = request == null ? null : request.getType();
        postLikeService.react(postId, type);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> removeReaction(@PathVariable Long postId) {
        postLikeService.removeReaction(postId);
        return ResponseEntity.noContent().build();
    }
}
