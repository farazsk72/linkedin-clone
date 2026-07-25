package com.codingshuttle.linkedInProject.notification_service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Validates the access token presented on the STOMP CONNECT frame. The gateway
 * already validates every REST call, but the WebSocket upgrade goes to a route
 * with no auth filter (a browser cannot attach an Authorization header to the
 * handshake), so the token is checked here instead - at CONNECT time, using the
 * same signing secret the gateway and userService share.
 */
@Service
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secretKey}") String jwtSecretKey) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @return the user id in the token's subject.
     * @throws io.jsonwebtoken.JwtException if the token is unsigned, tampered,
     *         expired, or its subject is not a number.
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }
}
