package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.postsService.entity.Post;
import com.codingshuttle.linkedInProject.postsService.entity.PostLike;
import com.codingshuttle.linkedInProject.postsService.entity.ReactionType;
import com.codingshuttle.linkedInProject.postsService.event.PostLiked;
import com.codingshuttle.linkedInProject.postsService.exception.BadRequestException;
import com.codingshuttle.linkedInProject.postsService.exception.ResourceNotFoundException;
import com.codingshuttle.linkedInProject.postsService.repository.PostLikeRepository;
import com.codingshuttle.linkedInProject.postsService.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final ModelMapper modelMapper;
    private final OutboxWriter outboxWriter;

    /**
     * Sets the caller's reaction on a post, inserting it or changing its type.
     * A user has at most one reaction per post, so reacting again replaces the
     * previous one. The owner is notified only when the reaction is new or its
     * type changed - re-sending the same reaction is a silent no-op, so an
     * idempotent client retry does not spam the owner.
     */
    @Transactional
    public void react(Long postId, ReactionType type) {
        Long userId = AuthContextHolder.getCurrentUserId();
        ReactionType reaction = type == null ? ReactionType.LIKE : type;
        log.info("User with ID: {} reacting {} to post with ID: {}", userId, reaction, postId);

        Post post = postRepository.findById(postId).orElseThrow(()
                -> new ResourceNotFoundException("Post not found with ID: "+postId));

        Optional<PostLike> existing = postLikeRepository.findByUserIdAndPostId(userId, postId);
        if(existing.isPresent() && existing.get().getType() == reaction) {
            return; // unchanged - nothing to persist, no one to notify
        }

        PostLike postLike = existing.orElseGet(() -> {
            PostLike fresh = new PostLike();
            fresh.setPostId(postId);
            fresh.setUserId(userId);
            return fresh;
        });
        postLike.setType(reaction);
        postLikeRepository.save(postLike);

        // Queue the notification in the outbox, in this same transaction: if
        // the reaction commits the event is guaranteed to be delivered, and if
        // the transaction rolls back the event rolls back with it.
        PostLiked postLiked = PostLiked.builder()
                .postId(postId)
                .likedByUserId(userId)
                .ownerUserId(post.getUserId())
                .reactionType(reaction.name())
                .build();
        outboxWriter.write("post_liked_topic", postId, postLiked);
    }

    /** Removes the caller's reaction from a post. */
    @Transactional
    public void removeReaction(Long postId) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("User with ID: {} removing reaction from post with ID: {}", userId, postId);

        postRepository.findById(postId).orElseThrow(()
                -> new ResourceNotFoundException("Post not found with ID: "+postId));

        boolean hasReacted = postLikeRepository.existsByUserIdAndPostId(userId, postId);
        if(!hasReacted) throw new BadRequestException("You have not reacted to this post");

        postLikeRepository.deleteByUserIdAndPostId(userId, postId);
    }

    // --- Backward-compatible aliases: a plain like is a LIKE reaction ---

    @Transactional
    public void likePost(Long postId) {
        react(postId, ReactionType.LIKE);
    }

    @Transactional
    public void unlikePost(Long postId) {
        removeReaction(postId);
    }
}
