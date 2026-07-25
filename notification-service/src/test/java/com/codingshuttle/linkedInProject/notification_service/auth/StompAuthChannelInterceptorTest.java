package com.codingshuttle.linkedInProject.notification_service.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The interceptor is the whole of the WebSocket auth story, so these pin its
 * contract: a good token binds a Principal, anything wrong fails the CONNECT,
 * and non-CONNECT frames pass through untouched (they inherit the session's
 * already-bound Principal).
 */
class StompAuthChannelInterceptorTest {

    private static final String SECRET =
            "test-secret-key-that-is-definitely-long-enough-for-hs-signing-1234";

    private final JwtService jwtService = new JwtService(SECRET);
    private final StompAuthChannelInterceptor interceptor =
            new StompAuthChannelInterceptor(jwtService);

    private String validToken(String subject) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 60_000))
                .signWith(key)
                .compact();
    }

    /** Mirrors how the inbound channel delivers a frame: a mutable accessor, so
     *  an interceptor can attach the Principal in place. */
    private Message<byte[]> frame(StompCommand command, String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (authHeader != null) {
            accessor.setNativeHeader("Authorization", authHeader);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private StompHeaderAccessor accessorOf(Message<?> message) {
        return MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    }

    @Test
    void bindsUserPrincipalOnAValidConnect() {
        Message<?> out = interceptor.preSend(
                frame(StompCommand.CONNECT, "Bearer " + validToken("7")), null);

        assertThat(accessorOf(out).getUser()).isNotNull();
        assertThat(accessorOf(out).getUser().getName()).isEqualTo("7");
    }

    @Test
    void rejectsConnectWithoutAnAuthorizationHeader() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void rejectsConnectWithAMalformedAuthorizationHeader() {
        assertThatThrownBy(() ->
                interceptor.preSend(frame(StompCommand.CONNECT, validToken("7")), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void rejectsConnectWithAnInvalidToken() {
        assertThatThrownBy(() ->
                interceptor.preSend(frame(StompCommand.CONNECT, "Bearer not-a-real-jwt"), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void leavesNonConnectFramesUntouched() {
        Message<?> out = interceptor.preSend(frame(StompCommand.SEND, null), null);

        // No exception, and no Principal attached - a SEND rides the session's
        // existing authentication rather than re-presenting the token.
        assertThat(accessorOf(out).getUser()).isNull();
    }
}
