package com.codingshuttle.linkedInProject.postsService.config;

import feign.micrometer.MicrometerObservationCapability;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the Feign observation capability explicitly rather than relying on
 * auto-configuration, so every Feign client wraps its calls in an observation.
 * That observation is what the tracing bridge uses to inject the trace context
 * (B3 headers) into the outgoing request - which is what lets the connections
 * service continue the caller's trace instead of starting a new one.
 */
@Configuration
public class FeignObservationConfig {

    @Bean
    @ConditionalOnBean(ObservationRegistry.class)
    @ConditionalOnMissingBean
    public MicrometerObservationCapability micrometerObservationCapability(ObservationRegistry registry) {
        return new MicrometerObservationCapability(registry);
    }
}
