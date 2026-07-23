package com.codingshuttle.linkedInProject.APIGateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A sliding-window limiter for the auth routes, so signup and login cannot be
 * hammered to brute-force a password or mint tokens in bulk.
 *
 * Deliberately in-memory: the counters live in this JVM, so with more than one
 * gateway instance each enforces its own quota and the effective limit
 * multiplies by the instance count. That is the point at which this should move
 * to Spring Cloud Gateway's Redis RequestRateLimiter; for a single instance it
 * is honest protection without the extra dependency.
 */
@Slf4j
@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    @Value("${ratelimit.requests:10}")
    private int maxRequests;

    @Value("${ratelimit.windowSeconds:60}")
    private int windowSeconds;

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    public RateLimitFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Preflight carries no credentials and cannot brute-force anything.
            if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
                return chain.filter(exchange);
            }

            String key = clientKey(exchange);
            if (isOverLimit(key)) {
                log.warn("Rate limit exceeded for {} on {}", key, exchange.getRequest().getURI().getPath());
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(windowSeconds));
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };
    }

    private synchronized boolean isOverLimit(String key) {
        Instant cutoff = Instant.now().minus(Duration.ofSeconds(windowSeconds));
        Deque<Instant> window = hits.computeIfAbsent(key, (k) -> new ArrayDeque<>());

        // Drop everything that has aged out before deciding, which is what
        // makes the window slide rather than reset on a fixed boundary.
        while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
            window.pollFirst();
        }

        if (window.size() >= maxRequests) {
            return true;
        }
        window.addLast(Instant.now());

        // Without this the map grows one entry per distinct client, forever.
        if (hits.size() > 10_000) {
            hits.entrySet().removeIf((entry) -> entry.getValue().isEmpty()
                    || entry.getValue().peekLast().isBefore(cutoff));
        }
        return false;
    }

    /**
     * X-Forwarded-For first, since behind a proxy every request would otherwise
     * share the proxy's address and one client could lock everyone out.
     */
    private String clientKey(org.springframework.web.server.ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

    static class Config {

    }
}
