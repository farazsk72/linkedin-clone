package com.codingshuttle.linkedInProject.userService.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
