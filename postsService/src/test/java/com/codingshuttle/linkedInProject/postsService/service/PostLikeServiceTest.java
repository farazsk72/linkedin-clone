package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.auth.AuthTestSupport;
import com.codingshuttle.linkedInProject.postsService.entity.Post;
import com.codingshuttle.linkedInProject.postsService.entity.PostLike;
import com.codingshuttle.linkedInProject.postsService.entity.ReactionType;
import com.codingshuttle.linkedInProject.postsService.event.PostLiked;
import com.codingshuttle.linkedInProject.postsService.exception.BadRequestException;
import com.codingshuttle.linkedInProject.postsService.exception.ResourceNotFoundException;
import com.codingshuttle.linkedInProject.postsService.repository.PostLikeRepository;
import com.codingshuttle.linkedInProject.postsService.repository.PostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The reaction upsert rules: a new reaction and a changed one both persist and
 * notify, an unchanged one does neither (so an idempotent retry never spams the
 * owner), and removal deletes only an existing reaction.
 */
@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    private static final long USER_ID = 7L;
    private static final long POST_ID = 5L;
    private static final long OWNER_ID = 20L;

    @Mock PostLikeRepository postLikeRepository;
    @Mock PostRepository postRepository;
    @Mock ModelMapper modelMapper;
    @Mock OutboxWriter outboxWriter;

    @InjectMocks PostLikeService postLikeService;

    private Post post;

    @BeforeEach
    void setUp() {
        AuthTestSupport.setCurrentUser(USER_ID);
        post = new Post();
        post.setId(POST_ID);
        post.setUserId(OWNER_ID);
    }

    @AfterEach
    void tearDown() {
        AuthTestSupport.clear();
    }

    private PostLike existingReaction(ReactionType type) {
        PostLike like = new PostLike();
        like.setPostId(POST_ID);
        like.setUserId(USER_ID);
        like.setType(type);
        return like;
    }

    @Test
    @DisplayName("a new reaction is saved and an event is queued")
    void newReactionPersistsAndNotifies() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postLikeRepository.findByUserIdAndPostId(USER_ID, POST_ID)).thenReturn(Optional.empty());

        postLikeService.react(POST_ID, ReactionType.CELEBRATE);

        ArgumentCaptor<PostLike> saved = ArgumentCaptor.forClass(PostLike.class);
        verify(postLikeRepository).save(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(ReactionType.CELEBRATE);
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);

        ArgumentCaptor<PostLiked> event = ArgumentCaptor.forClass(PostLiked.class);
        verify(outboxWriter).write(eq("post_liked_topic"), eq(POST_ID), event.capture());
        assertThat(event.getValue().getReactionType()).isEqualTo("CELEBRATE");
        assertThat(event.getValue().getOwnerUserId()).isEqualTo(OWNER_ID);
        assertThat(event.getValue().getLikedByUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("null type defaults to LIKE")
    void nullTypeDefaultsToLike() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postLikeRepository.findByUserIdAndPostId(USER_ID, POST_ID)).thenReturn(Optional.empty());

        postLikeService.react(POST_ID, null);

        ArgumentCaptor<PostLike> saved = ArgumentCaptor.forClass(PostLike.class);
        verify(postLikeRepository).save(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(ReactionType.LIKE);
    }

    @Test
    @DisplayName("changing the reaction type persists and notifies")
    void changedReactionPersistsAndNotifies() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postLikeRepository.findByUserIdAndPostId(USER_ID, POST_ID))
                .thenReturn(Optional.of(existingReaction(ReactionType.LIKE)));

        postLikeService.react(POST_ID, ReactionType.INSIGHTFUL);

        ArgumentCaptor<PostLike> saved = ArgumentCaptor.forClass(PostLike.class);
        verify(postLikeRepository).save(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(ReactionType.INSIGHTFUL);
        verify(outboxWriter).write(eq("post_liked_topic"), eq(POST_ID), any(PostLiked.class));
    }

    @Test
    @DisplayName("re-sending the same reaction is a no-op: no save, no event")
    void unchangedReactionDoesNothing() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postLikeRepository.findByUserIdAndPostId(USER_ID, POST_ID))
                .thenReturn(Optional.of(existingReaction(ReactionType.SUPPORT)));

        postLikeService.react(POST_ID, ReactionType.SUPPORT);

        verify(postLikeRepository, never()).save(any());
        verify(outboxWriter, never()).write(any(), any(), any());
    }

    @Test
    @DisplayName("reacting to a missing post is rejected")
    void reactingToMissingPostThrows() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postLikeService.react(POST_ID, ReactionType.LIKE))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postLikeRepository, never()).save(any());
        verify(outboxWriter, never()).write(any(), any(), any());
    }

    @Test
    @DisplayName("removing an existing reaction deletes it")
    void removeExistingReactionDeletes() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByUserIdAndPostId(USER_ID, POST_ID)).thenReturn(true);

        postLikeService.removeReaction(POST_ID);

        verify(postLikeRepository).deleteByUserIdAndPostId(USER_ID, POST_ID);
    }

    @Test
    @DisplayName("removing a reaction that was never left is rejected")
    void removeMissingReactionThrows() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByUserIdAndPostId(USER_ID, POST_ID)).thenReturn(false);

        assertThatThrownBy(() -> postLikeService.removeReaction(POST_ID))
                .isInstanceOf(BadRequestException.class);

        verify(postLikeRepository, never()).deleteByUserIdAndPostId(any(), any());
    }
}
