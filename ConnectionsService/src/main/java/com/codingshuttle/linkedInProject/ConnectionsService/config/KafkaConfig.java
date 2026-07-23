package com.codingshuttle.linkedInProject.ConnectionsService.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * These topics are owned by userService, which declares them identically. They
 * are declared here as well because a consumer that subscribes to a missing
 * topic makes the broker auto-create it with num.partitions=1. If this service
 * starts first, the group is then assigned that single partition, and the
 * assignment stays stale after userService widens the topic to 3 - so events
 * landing on partitions 1 and 2 go unconsumed until the next metadata refresh
 * forces a rebalance, minutes later.
 *
 * Declaring them here means the topic exists at full width before the listener
 * containers start, whichever service boots first.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic userCreatedTopic() {
        return new NewTopic("user_created_topic", 3, (short) 1);
    }

    @Bean
    public NewTopic userUpdatedTopic() {
        return new NewTopic("user_updated_topic", 3, (short) 1);
    }
}
