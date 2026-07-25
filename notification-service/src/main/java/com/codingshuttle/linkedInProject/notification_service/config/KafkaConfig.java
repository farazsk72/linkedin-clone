package com.codingshuttle.linkedInProject.notification_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * These topics are owned by postsService, which declares them identically. They
 * are declared here too because a consumer subscribing to a topic that does not
 * exist yet makes the broker auto-create it with num.partitions=1. If this
 * service starts first, the group is assigned that single partition and the
 * assignment stays stale after postsService widens the topic to 3 - events on
 * partitions 1 and 2 then go unconsumed until a metadata refresh forces a
 * rebalance, minutes later.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic postCreated() {
        return new NewTopic("post_created_topic", 3, (short) 1);
    }

    @Bean
    public NewTopic postLiked() {
        return new NewTopic("post_liked_topic", 3, (short) 1);
    }

    @Bean
    public NewTopic postCommented() {
        return new NewTopic("post_commented_topic", 3, (short) 1);
    }

    @Bean
    public NewTopic postReposted() {
        return new NewTopic("post_reposted_topic", 3, (short) 1);
    }

    @Bean
    public NewTopic userMentioned() {
        return new NewTopic("user_mentioned_topic", 3, (short) 1);
    }
}
