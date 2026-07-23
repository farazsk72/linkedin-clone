package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.client.ConnectionsGateway;
import com.codingshuttle.linkedInProject.postsService.dto.PersonDto;
import com.codingshuttle.linkedInProject.postsService.entity.Post;
import com.codingshuttle.linkedInProject.postsService.event.PostCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publishing of "post created" fan-out events, shared by the creation saga and
 * the draft-publish path so there is one place that knows the notification
 * contract.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventPublisher {

    private final ConnectionsGateway connectionsGateway;
    private final OutboxWriter outboxWriter;

    public void notifyConnections(Post post, Long ownerUserId) {
        List<PersonDto> connections = connectionsGateway.getFirstDegreeConnections(ownerUserId);

        // One outbox row per recipient. When called from within a transaction
        // (publishDraft) these commit atomically with the publish; the relay
        // then delivers them.
        for (PersonDto person : connections) {
            PostCreated postCreated = PostCreated.builder()
                    .postId(post.getId())
                    .content(post.getContent())
                    .userId(person.getUserId())
                    .ownerUserId(ownerUserId)
                    .build();
            outboxWriter.write("post_created_topic", post.getId(), postCreated);
        }
        log.info("queued post_created fan-out for post {} to {} connection(s)",
                post.getId(), connections.size());
    }
}
