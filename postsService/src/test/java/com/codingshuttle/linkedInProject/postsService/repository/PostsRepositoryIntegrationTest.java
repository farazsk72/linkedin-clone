package com.codingshuttle.linkedInProject.postsService.repository;

import com.codingshuttle.linkedInProject.postsService.entity.OutboxEvent;
import com.codingshuttle.linkedInProject.postsService.entity.PostLike;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Limit;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice tests against a REAL Postgres (via Testcontainers), not H2.
 * This is what makes the batch JPQL - the grouped count and the "which of these
 * did I like" projection that replaced the N+1 - trustworthy: it runs on the
 * same database the service uses in production.
 *
 * Requires a running Docker engine.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PostsRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired PostLikeRepository postLikeRepository;
    @Autowired OutboxRepository outboxRepository;

    private PostLike like(long userId, long postId) {
        PostLike pl = new PostLike();
        pl.setUserId(userId);
        pl.setPostId(postId);
        return pl;
    }

    @Test
    @DisplayName("countByPostIdIn returns one grouped row per liked post, omitting posts with no likes")
    void countByPostIdInGroupsAndOmitsZero() {
        // post 10 -> 2 likes, post 11 -> 1 like, post 12 -> 0 likes
        postLikeRepository.saveAll(List.of(
                like(1L, 10L), like(2L, 10L),
                like(1L, 11L)));

        List<Object[]> rows = postLikeRepository.countByPostIdIn(List.of(10L, 11L, 12L));

        Map<Long, Long> counts = rows.stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        assertThat(counts).containsOnly(
                org.assertj.core.data.MapEntry.entry(10L, 2L),
                org.assertj.core.data.MapEntry.entry(11L, 1L));
        assertThat(counts).doesNotContainKey(12L); // no likes -> absent, not zero
    }

    @Test
    @DisplayName("findLikedPostIds returns only the posts this user liked, within the given set")
    void findLikedPostIdsScopedToUserAndSet() {
        postLikeRepository.saveAll(List.of(
                like(1L, 10L), like(1L, 11L),   // user 1 liked 10 and 11
                like(2L, 12L)));                // user 2 liked 12

        List<Long> liked = postLikeRepository.findLikedPostIds(1L, List.of(10L, 11L, 12L));

        assertThat(liked).containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    @DisplayName("outbox: unprocessed events are returned oldest-first and counted")
    void outboxFindsUnprocessedOldestFirst() {
        OutboxEvent a = outboxRepository.save(event("A"));
        OutboxEvent b = outboxRepository.save(event("B"));
        // mark A processed
        a.setProcessedAt(java.time.LocalDateTime.now());
        outboxRepository.save(a);

        List<OutboxEvent> pending = outboxRepository.findByProcessedAtIsNullOrderByIdAsc(Limit.of(10));

        assertThat(pending).extracting(OutboxEvent::getId).containsExactly(b.getId());
        assertThat(outboxRepository.countByProcessedAtIsNull()).isEqualTo(1L);
    }

    private OutboxEvent event(String payload) {
        OutboxEvent e = new OutboxEvent();
        e.setTopic("some_topic");
        e.setMessageKey(1L);
        e.setEventType("com.example.SomeEvent");
        e.setPayload(payload);
        return e;
    }
}
