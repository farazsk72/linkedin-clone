package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.entity.OutboxEvent;
import com.codingshuttle.linkedInProject.postsService.event.PostLiked;
import com.codingshuttle.linkedInProject.postsService.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-tests the outbox relay's two important behaviours: it publishes pending
 * events and marks them processed, and it leaves them pending (for retry) when
 * the broker is unreachable.
 */
@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock OutboxRepository outboxRepository;
    @Mock KafkaTemplate<Long, Object> kafkaTemplate;

    // Mirror the production (Spring-auto-configured) mapper: it registers the
    // parameter-names module, without which the @Builder events - which have no
    // default constructor - cannot be deserialized by the relay.
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private OutboxRelay relay;
    private OutboxEvent pending;

    @BeforeEach
    void setUp() throws Exception {
        relay = new OutboxRelay(outboxRepository, kafkaTemplate, objectMapper);

        PostLiked event = PostLiked.builder().postId(5L).ownerUserId(2L).likedByUserId(3L).build();
        pending = new OutboxEvent();
        pending.setId(1L);
        pending.setTopic("post_liked_topic");
        pending.setMessageKey(5L);
        pending.setEventType(PostLiked.class.getName());
        pending.setPayload(objectMapper.writeValueAsString(event));

        when(outboxRepository.findByProcessedAtIsNullOrderByIdAsc(any(Limit.class)))
                .thenReturn(List.of(pending));
    }

    @Test
    @DisplayName("publishes the pending event and marks it processed")
    void publishesAndMarksProcessed() {
        doReturn(CompletableFuture.completedFuture(null))
                .when(kafkaTemplate).send(any(), any(), any());

        relay.publishPending();

        verify(kafkaTemplate).send(eq("post_liked_topic"), eq(5L), any(PostLiked.class));
        assertThat(pending.getProcessedAt()).isNotNull();
        verify(outboxRepository).save(pending);
    }

    @Test
    @DisplayName("leaves the event pending when the broker send fails")
    void leavesPendingOnBrokerFailure() {
        doReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")))
                .when(kafkaTemplate).send(any(), any(), any());

        relay.publishPending();

        assertThat(pending.getProcessedAt()).isNull();
        verify(outboxRepository, never()).save(any());
    }
}
