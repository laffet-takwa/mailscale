package com.mailscale.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long expirySeconds;

    public JwtService(
            @Value("${mailscale.jwt.secret}") String secret,
            @Value("${mailscale.jwt.expiry-seconds}") long expirySeconds) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            log.warn("JWT secret is shorter than 32 bytes - use a stronger secret in production");
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, Math.min(bytes.length, 32));
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirySeconds = expirySeconds;
    }

    public String createToken(User user, Organization organization, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("orgId", organization.getId())
                .claim("orgSlug", organization.getSlug())
                .claim("role", role.name())
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isExpired(String token) {
        try {
            parse(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public record TokenClaims(Long userId, String email, Long orgId, String orgSlug,
                               Role role, String jti) {

        public static TokenClaims from(Claims claims) {
            return new TokenClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    Long.valueOf(String.valueOf(claims.get("orgId"))),
                    claims.get("orgSlug", String.class),
                    Role.valueOf(claims.get("role", String.class)),
                    claims.get("jti", String.class)
            );
        }
    }

    public List<String> getRoles(String token) {
        return List.of(parse(token).get("role", String.class));
    }
}
