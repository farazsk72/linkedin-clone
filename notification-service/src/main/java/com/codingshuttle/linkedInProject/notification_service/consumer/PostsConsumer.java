package com.codingshuttle.linkedInProject.notification_service.consumer;

import com.codingshuttle.linkedInProject.notification_service.entity.Notification;
import com.codingshuttle.linkedInProject.notification_service.service.NotificationService;
import com.codingshuttle.linkedInProject.postsService.event.PostCommented;
import com.codingshuttle.linkedInProject.postsService.event.PostCreated;
import com.codingshuttle.linkedInProject.postsService.event.PostLiked;
import com.codingshuttle.linkedInProject.postsService.event.PostReposted;
import com.codingshuttle.linkedInProject.postsService.event.UserMentioned;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostsConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "post_created_topic")
    public void handlePostCreated(PostCreated postCreated) {
        log.info("handlePostCreated: {}", postCreated);

        String message = String.format("Your connection with id: %d has created this post: %s",
                postCreated.getOwnerUserId(), postCreated.getContent());
        Notification notification = Notification.builder()
                .message(message)
                .userId(postCreated.getUserId())
                .type("POST_CREATED")
                .targetId(postCreated.getPostId())
                .build();
        notificationService.addNotification(notification);
    }

    @KafkaListener(topics = "post_liked_topic")
    public void handlePostLiked(PostLiked postLiked) {
        log.info("handlePostLiked: {}", postLiked);

        String message = String.format("User with id: %d %s your post with id: %d",
                postLiked.getLikedByUserId(), reactionVerb(postLiked.getReactionType()),
                postLiked.getPostId());

        Notification notification = Notification.builder()
                .message(message)
                .userId(postLiked.getOwnerUserId())
                .type("POST_LIKED")
                .targetId(postLiked.getPostId())
                .build();
        notificationService.addNotification(notification);
    }

    @KafkaListener(topics = "user_mentioned_topic")
    public void handleUserMentioned(UserMentioned userMentioned) {
        log.info("handleUserMentioned: {}", userMentioned);

        String where = "COMMENT".equals(userMentioned.getContext()) ? "a comment" : "a post";
        String message = String.format("User with id: %d mentioned you in %s (post id: %d)",
                userMentioned.getMentionedByUserId(), where, userMentioned.getPostId());

        Notification notification = Notification.builder()
                .message(message)
                .userId(userMentioned.getMentionedUserId())
                .type("USER_MENTIONED")
                .targetId(userMentioned.getPostId())
                .build();
        notificationService.addNotification(notification);
    }

    /** Phrasing for each reaction. Null (older events) reads as a plain like. */
    private String reactionVerb(String reactionType) {
        if (reactionType == null) return "has liked";
        return switch (reactionType) {
            case "CELEBRATE" -> "celebrated";
            case "SUPPORT" -> "supported";
            case "INSIGHTFUL" -> "found insightful";
            case "FUNNY" -> "laughed at";
            default -> "has liked";
        };
    }

    @KafkaListener(topics = "post_commented_topic")
    public void handlePostCommented(PostCommented postCommented) {
        log.info("handlePostCommented: {}", postCommented);

        String message = String.format("User with id: %d commented on your post with id: %d: %s",
                postCommented.getCommentedByUserId(), postCommented.getPostId(), postCommented.getContent());

        Notification notification = Notification.builder()
                .message(message)
                .userId(postCommented.getOwnerUserId())
                .type("POST_COMMENTED")
                .targetId(postCommented.getPostId())
                .build();
        notificationService.addNotification(notification);
    }

    @KafkaListener(topics = "post_reposted_topic")
    public void handlePostReposted(PostReposted postReposted) {
        log.info("handlePostReposted: {}", postReposted);

        String message = String.format("User with id: %d reshared your post with id: %d",
                postReposted.getRepostedByUserId(), postReposted.getPostId());

        Notification notification = Notification.builder()
                .message(message)
                .userId(postReposted.getOwnerUserId())
                .type("POST_REPOSTED")
                .targetId(postReposted.getPostId())
                .build();
        notificationService.addNotification(notification);
    }
}












