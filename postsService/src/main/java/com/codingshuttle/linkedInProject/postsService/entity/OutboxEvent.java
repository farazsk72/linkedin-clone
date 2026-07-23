package com.codingshuttle.linkedInProject.postsService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A pending domain event, written in the SAME transaction as the business
 * change that produced it. A separate relay publishes it to Kafka and stamps
 * processedAt. This is the transactional-outbox pattern: it removes the
 * dual-write race where the DB commits but the Kafka publish fails (or vice
 * versa), so an event is never silently lost or emitted for a change that
 * rolled back.
 */
@Entity
@Getter
@Setter
@Table(name = "outbox_events", indexes = @Index(name = "idx_outbox_unprocessed", columnList = "processedAt"))
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    /** Kafka message key (nullable) - kept as the aggregate id for ordering. */
    private Long messageKey;

    /** Fully-qualified event class, so the relay can rebuild the exact type. */
    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Null until the relay has published it. */
    private LocalDateTime processedAt;
}
