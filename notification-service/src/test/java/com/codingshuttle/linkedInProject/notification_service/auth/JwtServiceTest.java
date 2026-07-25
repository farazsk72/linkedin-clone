package com.codingshuttle.linkedInProject.notification_service.auth;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The STOMP handshake trusts this service to validate the token itself, so these
 * cover the three ways a token can be untrustworthy: wrong signature, expiry,
 * and (implicitly) a valid one whose subject is read back out.
 */
class JwtServiceTest {

    private static final String SECRET =
            "test-secret-key-that-is-definitely-long-enough-for-hs-signing-1234";
    private static final String OTHER_SECRET =
            "a-different-secret-key-also-long-enough-to-be-valid-for-hs-9876";

    private final JwtService jwtService = new JwtService(SECRET);

    private SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String token(String subject, SecretKey signingKey, long ttlMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMillis))
                .signWith(signingKey)
                .compact();
    }

    @Test
    void returnsUserIdFromTheSubjectOfAValidToken() {
        String jwt = token("42", key(SECRET), 60_000);
        assertThat(jwtService.getUserIdFromToken(jwt)).isEqualTo(42L);
    }

    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        String jwt = token("42", key(OTHER_SECRET), 60_000);
        assertThatThrownBy(() -> jwtService.getUserIdFromToken(jwt))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsAnExpiredToken() {
        String jwt = token("42", key(SECRET), -1_000);
        assertThatThrownBy(() -> jwtService.getUserIdFromToken(jwt))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
