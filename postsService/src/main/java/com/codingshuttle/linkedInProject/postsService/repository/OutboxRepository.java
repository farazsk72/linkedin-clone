package com.codingshuttle.linkedInProject.postsService.repository;

import com.codingshuttle.linkedInProject.postsService.entity.OutboxEvent;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /** The oldest still-unpublished events, a bounded batch at a time. */
    List<OutboxEvent> findByProcessedAtIsNullOrderByIdAsc(Limit limit);

    long countByProcessedAtIsNull();
}
