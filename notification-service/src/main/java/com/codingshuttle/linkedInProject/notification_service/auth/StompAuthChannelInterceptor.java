package com.codingshuttle.linkedInProject.notification_service.auth;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Authenticates a STOMP session on its CONNECT frame. The WebSocket route on the
 * gateway carries no auth filter - the browser cannot put an Authorization header
 * on the handshake - so the token instead rides in a STOMP header on CONNECT,
 * which the client <em>can</em> set. A valid token binds a {@link StompPrincipal}
 * to the session; everything after CONNECT is then addressable per user, and a
 * missing or bad token fails the connection before any subscription is allowed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Only the CONNECT frame is authenticated; later frames reuse the
        // Principal already bound to the session at connect time.
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new MessagingException("Missing or malformed Authorization header on CONNECT");
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        try {
            Long userId = jwtService.getUserIdFromToken(token);
            accessor.setUser(new StompPrincipal(String.valueOf(userId)));
            log.debug("STOMP CONNECT authenticated for user {}", userId);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Rejected STOMP CONNECT: {}", e.getMessage());
            throw new MessagingException("Invalid JWT on CONNECT");
        }

        return message;
    }
}
