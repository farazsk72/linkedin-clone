package com.codingshuttle.linkedInProject.userService.service;

import com.codingshuttle.linkedInProject.userService.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secretKey}")
    private String jwtSecretKey;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Short-lived on purpose. The gateway validates signature and expiry only,
     * with no revocation list to consult, so this window IS the blast radius of
     * a leaked or already-issued token. Sessions survive via refresh tokens,
     * which can be revoked because they are looked up in the database.
     */
    private static final long ACCESS_TOKEN_MILLIS = 1000L * 60 * 15;

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_MILLIS))
                .signWith(getSecretKey())
                .compact();
    }

}