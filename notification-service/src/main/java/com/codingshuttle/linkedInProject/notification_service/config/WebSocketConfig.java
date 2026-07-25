package com.codingshuttle.linkedInProject.notification_service.config;

import com.codingshuttle.linkedInProject.notification_service.auth.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket wiring for live notification delivery.
 *
 * <p>Clients connect at {@code /ws} (served at {@code /notifications/ws} because
 * of the service context-path) and subscribe to {@code /user/queue/notifications}.
 * The broker is the in-process simple broker - fine for a single instance; a
 * multi-instance deployment would swap {@code enableSimpleBroker} for a relay to
 * an external broker so a push reaches whichever instance holds the session.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Value("${cors.allowed-origin:http://localhost:5173}")
    private String allowedOrigin;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS fallback so the connection survives proxies that will not
        // upgrade a raw WebSocket - it degrades to XHR streaming/polling.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigin)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /queue for per-user destinations, /topic left available for any
        // future broadcast. /user is the prefix convertAndSendToUser resolves.
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Authenticates CONNECT and binds the per-session Principal.
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
