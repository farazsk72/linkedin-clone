package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.auth.AuthTestSupport;
import com.codingshuttle.linkedInProject.postsService.client.UploaderGateway;
import com.codingshuttle.linkedInProject.postsService.dto.PostCreateRequestDto;
import com.codingshuttle.linkedInProject.postsService.dto.PostDto;
import com.codingshuttle.linkedInProject.postsService.entity.Post;
import com.codingshuttle.linkedInProject.postsService.exception.SagaFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The flagship unit test: it proves the saga's orchestration and compensation
 * logic with everything mocked, so no DB/Kafka/services are needed.
 */
@ExtendWith(MockitoExtension.class)
class PostCreationSagaTest {

    @Mock UploaderGateway uploaderGateway;
    @Mock PostPersistenceService persistenceService;
    @Mock PostEventPublisher eventPublisher;
    @Mock PostService postService;

    @InjectMocks PostCreationSaga saga;

    private static final Long USER_ID = 1L;
    private PostCreateRequestDto publishedRequest;

    @BeforeEach
    void setUp() {
        AuthTestSupport.setCurrentUser(USER_ID);
        publishedRequest = new PostCreateRequestDto();
        publishedRequest.setContent("hello");
    }

    @AfterEach
    void tearDown() {
        AuthTestSupport.clear();
    }

    private Post persistedPost(Long id, String status) {
        Post post = new Post();
        post.setId(id);
        post.setUserId(USER_ID);
        post.setContent("hello");
        post.setStatus(status);
        return post;
    }

    @Test
    @DisplayName("happy path: persists, publishes, and never compensates")
    void happyPath() {
        Post post = persistedPost(5L, "PUBLISHED");
        PostDto expected = new PostDto();
        when(persistenceService.persist(any(), eq(USER_ID), any())).thenReturn(post);
        when(postService.toDto(post, USER_ID)).thenReturn(expected);

        PostDto result = saga.create(publishedRequest, null, null);

        assertThat(result).isSameAs(expected);
        verify(persistenceService).persist(publishedRequest, USER_ID, null);
        verify(eventPublisher).notifyConnections(post, USER_ID);
        verify(persistenceService, never()).deletePostAndTags(anyLong());
    }

    @Test
    @DisplayName("failure at PUBLISH compensates the committed persist step")
    void publishFailureCompensatesPersist() {
        Post post = persistedPost(5L, "PUBLISHED");
        when(persistenceService.persist(any(), eq(USER_ID), any())).thenReturn(post);

        assertThatThrownBy(() -> saga.create(publishedRequest, null, "PUBLISH"))
                .isInstanceOf(SagaFailedException.class)
                .hasMessageContaining("rolled back");

        // persist committed, then the later step failed -> compensation deletes it
        verify(persistenceService).persist(any(), eq(USER_ID), any());
        verify(persistenceService).deletePostAndTags(5L);
        // the publish never ran (it is what failed)
        verify(eventPublisher, never()).notifyConnections(any(), anyLong());
    }

    @Test
    @DisplayName("failure at PERSIST (before commit) compensates nothing")
    void persistFailureCompensatesNothing() {
        assertThatThrownBy(() -> saga.create(publishedRequest, null, "PERSIST"))
                .isInstanceOf(SagaFailedException.class);

        verify(persistenceService, never()).persist(any(), anyLong(), any());
        verify(persistenceService, never()).deletePostAndTags(anyLong());
        verify(eventPublisher, never()).notifyConnections(any(), anyLong());
    }

    @Test
    @DisplayName("a draft persists but publishes nothing")
    void draftDoesNotPublish() {
        PostCreateRequestDto draft = new PostCreateRequestDto();
        draft.setContent("wip");
        draft.setDraft(true);
        Post post = persistedPost(7L, "DRAFT");
        when(persistenceService.persist(any(), eq(USER_ID), any())).thenReturn(post);
        when(postService.toDto(post, USER_ID)).thenReturn(new PostDto());

        saga.create(draft, null, null);

        verify(eventPublisher, never()).notifyConnections(any(), anyLong());
        verify(persistenceService, never()).deletePostAndTags(anyLong());
    }
}
