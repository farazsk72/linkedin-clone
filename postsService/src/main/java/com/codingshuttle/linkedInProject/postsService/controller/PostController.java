package com.codingshuttle.linkedInProject.postsService.controller;

import com.codingshuttle.linkedInProject.postsService.dto.PageResponse;
import com.codingshuttle.linkedInProject.postsService.dto.PostCreateRequestDto;
import com.codingshuttle.linkedInProject.postsService.dto.PostDto;
import com.codingshuttle.linkedInProject.postsService.dto.PostUpdateRequestDto;
import com.codingshuttle.linkedInProject.postsService.dto.RepostRequestDto;
import com.codingshuttle.linkedInProject.postsService.service.PostCreationSaga;
import com.codingshuttle.linkedInProject.postsService.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/core")
public class PostController {

    private final PostService postService;
    private final PostCreationSaga postCreationSaga;

    // Creation runs as an orchestrated saga. X-Saga-Fail-At (UPLOAD|PERSIST|
    // PUBLISH) is a test hook that forces that step to fail so the compensation
    // path can be exercised; it is absent in normal use.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostDto> createPost(@RequestPart("post") PostCreateRequestDto postCreateRequestDto,
                                              @RequestPart(value = "file", required = false) MultipartFile file,
                                              @RequestHeader(value = "X-Saga-Fail-At", required = false) String failAt) {
        PostDto postDto = postCreationSaga.create(postCreateRequestDto, file, failAt);
        return new ResponseEntity<>(postDto, HttpStatus.CREATED);
    }

    @GetMapping("/feed")
    public ResponseEntity<PageResponse<PostDto>> getFeed(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getFeed(page, size));
    }

    @GetMapping("/following-feed")
    public ResponseEntity<PageResponse<PostDto>> getFollowingFeed(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getFollowingFeed(page, size));
    }

    @GetMapping("/drafts")
    public ResponseEntity<PageResponse<PostDto>> getMyDrafts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getMyDrafts(page, size));
    }

    @PostMapping("/{postId}/publish")
    public ResponseEntity<PostDto> publishDraft(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.publishDraft(postId));
    }

    /** Tag browsing shows public posts only - see PostService for why. */
    @GetMapping("/tag/{tag}")
    public ResponseEntity<PageResponse<PostDto>> getPostsByHashtag(
            @PathVariable String tag,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getPostsByHashtag(tag, page, size));
    }

    @PostMapping("/{postId}/repost")
    public ResponseEntity<PostDto> repost(@PathVariable Long postId,
                                          @RequestBody(required = false) RepostRequestDto dto) {
        return new ResponseEntity<>(postService.repost(postId,
                dto == null ? new RepostRequestDto() : dto), HttpStatus.CREATED);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDto> getPost(@PathVariable Long postId) {
        PostDto postDto = postService.getPostById(postId);
        return ResponseEntity.ok(postDto);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostDto> updatePost(@PathVariable Long postId,
                                              @RequestBody PostUpdateRequestDto dto) {
        return ResponseEntity.ok(postService.updatePost(postId, dto));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/allPosts")
    public ResponseEntity<PageResponse<PostDto>> getAllPostsOfUser(
            @PathVariable Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getAllPostsOfUser(userId, page, size));
    }
}
