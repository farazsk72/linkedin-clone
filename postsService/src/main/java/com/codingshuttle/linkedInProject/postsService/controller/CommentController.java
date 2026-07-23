package com.codingshuttle.linkedInProject.postsService.controller;

import com.codingshuttle.linkedInProject.postsService.dto.CommentCreateRequestDto;
import com.codingshuttle.linkedInProject.postsService.dto.CommentDto;
import com.codingshuttle.linkedInProject.postsService.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{postId}")
    public ResponseEntity<CommentDto> addComment(@PathVariable Long postId,
                                                 @RequestBody CommentCreateRequestDto dto) {
        return new ResponseEntity<>(commentService.addComment(postId, dto), HttpStatus.CREATED);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getComments(postId));
    }

    // Keyed by comment id, not post id - the two paths above are keyed by post,
    // so this one deliberately lives under /single to keep them unambiguous.
    /** Author only - the post owner can delete a comment but not reword it. */
    @PutMapping("/single/{commentId}")
    public ResponseEntity<CommentDto> updateComment(@PathVariable Long commentId,
                                                    @RequestBody CommentCreateRequestDto dto) {
        return ResponseEntity.ok(commentService.updateComment(commentId, dto));
    }

    @DeleteMapping("/single/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/single/{commentId}/like")
    public ResponseEntity<CommentDto> likeComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.likeComment(commentId));
    }

    @DeleteMapping("/single/{commentId}/like")
    public ResponseEntity<CommentDto> unlikeComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.unlikeComment(commentId));
    }
}
