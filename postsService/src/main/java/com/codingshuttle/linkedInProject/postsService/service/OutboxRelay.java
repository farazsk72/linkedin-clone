package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.entity.OutboxEvent;
import com.codingshuttle.linkedInProject.postsService.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls the outbox and publishes pending events to Kafka, marking each as
 * processed only after the broker acknowledges it. If the broker is down the
 * row stays pending and is retried on the next tick - so an event survives a
 * broker outage that happened at write time. This is the "at-least-once"
 * delivery half of the outbox pattern.
 *
 * Rebuilding the concrete event type and sending it through the same
 * JsonSerializer the direct producers used means the wire format (including the
 * type header consumers rely on) is unchanged - no consumer needed touching.
 *
 * Single-instance assumption: with multiple posts-service replicas each would
 * poll and could double-publish. That is tolerable under at-least-once
 * semantics, but a production relay would claim rows with SELECT ... FOR UPDATE
 * SKIP LOCKED to avoid the duplicate work.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<Long, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:1000}")
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository.findByProcessedAtIsNullOrderByIdAsc(Limit.of(100));
        if (pending.isEmpty()) return;

        log.info("outbox relay: {} event(s) to publish", pending.size());
        int published = 0;
        for (OutboxEvent row : pending) {
            try {
                Object event = objectMapper.readValue(row.getPayload(), Class.forName(row.getEventType()));
                // Block on the ack so we only mark processed once Kafka has it.
                kafkaTemplate.send(row.getTopic(), row.getMessageKey(), event).get(10, TimeUnit.SECONDS);

                row.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(row);
                published++;
            } catch (Exception e) {
                // Leave it pending - the next tick retries. Stop the batch so a
                // hard-down broker does not spin through the whole table.
                log.warn("outbox relay: publish failed for event {} ({}), will retry: {}",
                        row.getId(), row.getTopic(), e.toString());
                break;
            }
        }
        if (published > 0) {
            log.info("outbox relay: published {} event(s)", published);
        }
    }
}
