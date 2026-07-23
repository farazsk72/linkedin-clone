package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.entity.OutboxEvent;
import com.codingshuttle.linkedInProject.postsService.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Writes an event to the outbox table. Callers invoke this from inside their
 * own @Transactional business method, so the outbox row commits atomically with
 * the business change - the whole point of the pattern. It does NOT talk to
 * Kafka; the {@link OutboxRelay} does that afterwards.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxWriter {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void write(String topic, Long messageKey, Object event) {
        OutboxEvent row = new OutboxEvent();
        row.setTopic(topic);
        row.setMessageKey(messageKey);
        row.setEventType(event.getClass().getName());
        try {
            row.setPayload(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            // Serialization failing means the event is malformed - fail the
            // whole business transaction rather than commit a change whose
            // event can never be delivered.
            throw new IllegalStateException("Could not serialise outbox event " + event.getClass(), e);
        }
        outboxRepository.save(row);
        log.debug("outbox: queued {} for topic {}", event.getClass().getSimpleName(), topic);
    }
}
