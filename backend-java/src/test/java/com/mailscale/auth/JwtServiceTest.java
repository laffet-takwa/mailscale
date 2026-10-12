package com.mailscale.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-0123456789abcdef", 3600);

    @Test
    void roundTripToken() {
        User user = new User("a@example.com", "hash", "A");
        user.setId(1L);
        Organization org = new Organization("Org", "org");
        org.setId(1L);
        String token = jwtService.createToken(user, org, Role.OWNER);

        JwtService.TokenClaims claims =
                JwtService.TokenClaims.from(jwtService.parse(token));
        assertEquals(1L, claims.userId());
        assertEquals("a@example.com", claims.email());
        assertEquals(1L, claims.orgId());
        assertEquals("org", claims.orgSlug());
        assertEquals(Role.OWNER, claims.role());
        assertNotNull(claims.jti());
    }

    @Test
    void tamperedTokenFails() {
        User user = new User("a@example.com", "hash", "A");
        user.setId(1L);
        Organization org = new Organization("Org", "org");
        org.setId(1L);
        String token = jwtService.createToken(user, org, Role.OWNER);
        String tampered = token.substring(0, token.length() - 3) + "xxx";
        assertThrows(Exception.class, () -> jwtService.parse(tampered));
    }

    @Test
    void wrongSecretFails() {
        User user = new User("a@example.com", "hash", "A");
        user.setId(1L);
        Organization org = new Organization("Org", "org");
        org.setId(1L);
        String token = jwtService.createToken(user, org, Role.OWNER);
        JwtService other = new JwtService(
                "different-secret-0123456789ab", 3600);
        assertThrows(Exception.class, () -> other.parse(token));
    }

    @Test
    void weakSecretIsPaddedNotRejected() {
        JwtService weak = new JwtService("short", 3600);
        assertNotNull(weak);
    }
}
