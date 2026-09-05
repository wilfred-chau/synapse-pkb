package com.wilfredchau.synapsepkb.security;

import com.wilfredchau.synapsepkb.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecurityProperties securityProperties;
    private final Key signingKey;

    public JwtService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        this.signingKey = Keys.hmacShaKeyFor(securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(securityProperties.getJwtExpirationMinutes() * 60);

        return Jwts.builder()
                .subject(user.username())
                .claim("uid", user.id())
                .claim("displayName", user.displayName())
                .claim("spaceKey", user.spaceKey())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new AuthenticatedUser(
                claims.get("uid", Long.class),
                claims.getSubject(),
                claims.get("displayName", String.class),
                claims.get("spaceKey", String.class));
    }
}
