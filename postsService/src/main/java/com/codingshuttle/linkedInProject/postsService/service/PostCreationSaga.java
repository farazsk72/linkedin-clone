package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.auth.AuthContextHolder;
import com.codingshuttle.linkedInProject.postsService.client.UploaderGateway;
import com.codingshuttle.linkedInProject.postsService.dto.PostCreateRequestDto;
import com.codingshuttle.linkedInProject.postsService.dto.PostDto;
import com.codingshuttle.linkedInProject.postsService.entity.Post;
import com.codingshuttle.linkedInProject.postsService.exception.SagaFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Orchestrated saga for creating a post. The three steps cross transactional
 * and service boundaries, so they cannot share one ACID transaction:
 *
 *   1. UPLOAD  - store the image in the uploader (external side effect)
 *   2. PERSIST - save the post row + hashtags (its own committed DB transaction)
 *   3. PUBLISH - fan out "post created" events to the author's connections
 *
 * Each successful step pushes a compensating action. If a later step fails,
 * the compensations run in reverse order (delete the post, then delete the
 * uploaded image), leaving no orphaned state - the pattern a distributed
 * rollback needs when a single transaction cannot span the steps.
 *
 * The optional {@code failAt} hook (wired to the X-Saga-Fail-At header) forces
 * a chosen step to fail, so the compensation path can be exercised on demand.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostCreationSaga {

    private static final String PUBLISHED = "PUBLISHED";

    private final UploaderGateway uploaderGateway;
    private final PostPersistenceService persistenceService;
    private final PostEventPublisher eventPublisher;
    private final PostService postService;

    private record Compensation(String name, Runnable action) {
    }

    public PostDto create(PostCreateRequestDto dto, MultipartFile file, String failAt) {
        Long userId = AuthContextHolder.getCurrentUserId();
        Deque<Compensation> compensations = new ArrayDeque<>();

        try {
            // --- Step 1: upload the image (skipped for text-only posts) ---
            String imageUrl = null;
            if (file != null && !file.isEmpty()) {
                failIfRequested(failAt, "UPLOAD");
                imageUrl = uploaderGateway.upload(file);
                final String uploaded = imageUrl;
                compensations.push(new Compensation("delete-image",
                        () -> uploaderGateway.deleteQuietly(uploaded)));
            }

            // --- Step 2: persist the post (commits in its own transaction) ---
            failIfRequested(failAt, "PERSIST");
            Post post = persistenceService.persist(dto, userId, imageUrl);
            compensations.push(new Compensation("delete-post",
                    () -> persistenceService.deletePostAndTags(post.getId())));

            // --- Step 3: publish the fan-out events ---
            failIfRequested(failAt, "PUBLISH");
            if (PUBLISHED.equals(post.getStatus())) {
                eventPublisher.notifyConnections(post, userId);
                eventPublisher.notifyMentions(post, userId);
            }

            log.info("post-creation saga completed for post {}", post.getId());
            return postService.toDto(post, userId);

        } catch (RuntimeException e) {
            log.warn("post-creation saga failed ({}); compensating {} completed step(s)",
                    e.getMessage(), compensations.size());
            compensate(compensations);
            throw new SagaFailedException("Post creation failed and was rolled back: " + e.getMessage());
        }
    }

    /** Runs compensations in reverse (LIFO); one failing must not stop the rest. */
    private void compensate(Deque<Compensation> compensations) {
        while (!compensations.isEmpty()) {
            Compensation c = compensations.pop();
            try {
                c.action().run();
            } catch (Exception e) {
                log.error("compensation '{}' failed - manual reconciliation may be needed: {}",
                        c.name(), e.toString());
            }
        }
    }

    private void failIfRequested(String failAt, String step) {
        if (step.equalsIgnoreCase(failAt)) {
            throw new IllegalStateException("simulated failure injected at step " + step);
        }
    }
}
