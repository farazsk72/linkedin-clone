package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.postsService.dto.PageResponse;
import com.codingshuttle.linkedInProject.postsService.dto.PostDto;
import com.codingshuttle.linkedInProject.postsService.entity.Post;
import com.codingshuttle.linkedInProject.postsService.entity.SavedPost;
import com.codingshuttle.linkedInProject.postsService.exception.ResourceNotFoundException;
import com.codingshuttle.linkedInProject.postsService.repository.PostRepository;
import com.codingshuttle.linkedInProject.postsService.repository.SavedPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedPostService {

    private final SavedPostRepository savedPostRepository;
    private final PostRepository postRepository;
    private final PostService postService;

    @Transactional
    public void savePost(Long postId) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("User with ID: {} saving the post with ID: {}", userId, postId);

        postRepository.findById(postId).orElseThrow(()
                -> new ResourceNotFoundException("Post not found with ID: "+postId));

        // Saving an already-saved post succeeds quietly. Unlike liking, there
        // is no meaningful error here - the user's intent is already satisfied.
        if(savedPostRepository.existsByUserIdAndPostId(userId, postId)) {
            return;
        }

        SavedPost savedPost = new SavedPost();
        savedPost.setUserId(userId);
        savedPost.setPostId(postId);
        savedPostRepository.save(savedPost);
    }

    @Transactional
    public void unsavePost(Long postId) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("User with ID: {} unsaving the post with ID: {}", userId, postId);

        savedPostRepository.deleteByUserIdAndPostId(userId, postId);
    }

    public PageResponse<PostDto> getSavedPosts(int page, int size) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Getting saved posts for user with ID: {}, page: {}", userId, page);

        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));

        Page<SavedPost> saved = savedPostRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);

        // Fetch the posts for the whole page at once, then rebuild the saved
        // order (findAllById does not preserve it) and hand the list to the
        // batch DTO builder - one aggregate load for the page rather than the
        // findById-then-per-post-lookups this used to do.
        List<Long> postIds = saved.getContent().stream().map(SavedPost::getPostId).toList();
        Map<Long, Post> posts = postRepository.findAllById(postIds).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        List<Post> ordered = new ArrayList<>(postIds.size());
        for(Long id : postIds) {
            Post post = posts.get(id);
            // Deleting a post clears its saved rows, so an orphan is unexpected;
            // preserve the original "fail the page" behaviour if one appears.
            if(post == null) {
                throw new ResourceNotFoundException("Saved post refers to a missing post with ID: "+id);
            }
            ordered.add(post);
        }

        return PageResponse.fromContent(saved, postService.toDtos(ordered, userId));
    }
}
