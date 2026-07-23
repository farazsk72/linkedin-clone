package com.codingshuttle.linkedInProject.postsService.controller;

import com.codingshuttle.linkedInProject.postsService.dto.PageResponse;
import com.codingshuttle.linkedInProject.postsService.dto.PostDto;
import com.codingshuttle.linkedInProject.postsService.service.SavedPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/saved")
public class SavedPostController {

    private final SavedPostService savedPostService;

    @GetMapping
    public ResponseEntity<PageResponse<PostDto>> getSavedPosts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(savedPostService.getSavedPosts(page, size));
    }

    @PostMapping("/{postId}")
    public ResponseEntity<Void> savePost(@PathVariable Long postId) {
        savedPostService.savePost(postId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> unsavePost(@PathVariable Long postId) {
        savedPostService.unsavePost(postId);
        return ResponseEntity.noContent().build();
    }
}
