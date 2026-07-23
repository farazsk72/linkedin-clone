package com.codingshuttle.linkedInProject.postsService.controller;

import com.codingshuttle.linkedInProject.postsService.dto.PageResponse;
import com.codingshuttle.linkedInProject.postsService.dto.PostDto;
import com.codingshuttle.linkedInProject.postsService.dto.TrendingHashtagDto;
import com.codingshuttle.linkedInProject.postsService.service.HashtagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hashtags")
public class HashtagController {

    private final HashtagService hashtagService;

    @GetMapping("/trending")
    public ResponseEntity<List<TrendingHashtagDto>> getTrending(
            @RequestParam(value = "days", defaultValue = "7") int days,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(hashtagService.getTrending(days, limit));
    }

    @GetMapping("/following")
    public ResponseEntity<List<String>> getFollowedTags() {
        return ResponseEntity.ok(hashtagService.getFollowedTags());
    }

    @GetMapping("/following/feed")
    public ResponseEntity<PageResponse<PostDto>> getFollowedTagFeed(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(hashtagService.getFollowedTagFeed(page, size));
    }

    @PostMapping("/{tag}/follow")
    public ResponseEntity<Void> followTag(@PathVariable String tag) {
        hashtagService.followTag(tag);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tag}/follow")
    public ResponseEntity<Void> unfollowTag(@PathVariable String tag) {
        hashtagService.unfollowTag(tag);
        return ResponseEntity.noContent().build();
    }
}
