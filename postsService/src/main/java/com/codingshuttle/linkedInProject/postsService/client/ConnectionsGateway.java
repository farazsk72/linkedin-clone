package com.codingshuttle.linkedInProject.postsService.client;

import com.codingshuttle.linkedInProject.postsService.dto.PersonDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resilient wrapper over the connections Feign client. Reads degrade to empty
 * rather than propagating a failure, which is the safe direction:
 *
 *  - an empty connection list means the feed shows only your own posts;
 *  - it also makes CONNECTIONS-only visibility checks fail closed (a post stays
 *    hidden when we cannot confirm the relationship), never leaking one.
 *
 * The circuit opens after repeated failures so a down connections service stops
 * being hammered and callers fail fast into the fallback instead of blocking.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectionsGateway {

    private final ConnectionsServiceClient client;

    @CircuitBreaker(name = "connections", fallbackMethod = "noConnections")
    public List<PersonDto> getFirstDegreeConnections(Long userId) {
        return client.getFirstDegreeConnections(userId);
    }

    @SuppressWarnings("unused") // referenced by name from the annotation
    private List<PersonDto> noConnections(Long userId, Throwable t) {
        log.warn("connections unavailable, treating user {} as having no connections: {}",
                userId, t.toString());
        return List.of();
    }

    @CircuitBreaker(name = "connections", fallbackMethod = "noFollowing")
    public List<Long> getFollowingIds() {
        return client.getFollowingIds();
    }

    @SuppressWarnings("unused")
    private List<Long> noFollowing(Throwable t) {
        log.warn("connections unavailable, treating caller as following no one: {}", t.toString());
        return List.of();
    }

    /** Whether the current caller is connected to {@code userId}. */
    @CircuitBreaker(name = "connections", fallbackMethod = "notConnected")
    public boolean isConnectedTo(Long userId) {
        return "CONNECTED".equals(client.getConnectionStatus(userId).getStatus());
    }

    @SuppressWarnings("unused")
    private boolean notConnected(Long userId, Throwable t) {
        // Fail closed: if we cannot confirm the connection, hide the post.
        log.warn("connections unavailable, cannot confirm link to {}, hiding: {}", userId, t.toString());
        return false;
    }
}
