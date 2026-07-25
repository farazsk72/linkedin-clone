package com.codingshuttle.linkedInProject.postsService.service;

import com.codingshuttle.linkedInProject.postsService.client.ConnectionsGateway;
import com.codingshuttle.linkedInProject.postsService.dto.PersonDto;
import com.codingshuttle.linkedInProject.postsService.entity.Post;
import com.codingshuttle.linkedInProject.postsService.event.PostCreated;
import com.codingshuttle.linkedInProject.postsService.event.UserMentioned;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

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

    /**
     * Notifies each user @mentioned in the post's text, skipping the author so
     * mentioning yourself never pings you. One outbox row per mentioned user.
     */
    public void notifyMentions(Post post, Long mentionerUserId) {
        Set<Long> mentioned = MentionExtractor.extract(post.getContent());
        int queued = 0;
        for (Long userId : mentioned) {
            if (userId.equals(mentionerUserId)) continue;
            UserMentioned event = UserMentioned.builder()
                    .mentionedUserId(userId)
                    .mentionedByUserId(mentionerUserId)
                    .postId(post.getId())
                    .context("POST")
                    .build();
            outboxWriter.write("user_mentioned_topic", post.getId(), event);
            queued++;
        }
        if (queued > 0) {
            log.info("queued {} mention notification(s) for post {}", queued, post.getId());
        }
    }
}
