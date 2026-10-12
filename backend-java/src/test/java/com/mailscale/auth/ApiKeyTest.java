package com.mailscale.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApiKeyTest {

    @Test
    void activeKeyIsUsable() {
        ApiKey key = new ApiKey(null, "test", "hash", "prefix",
                "email:send", Instant.now().plusSeconds(3600));
        assertTrue(key.isActive(Instant.now()));
    }

    @Test
    void expiredKeyIsNotUsable() {
        ApiKey key = new ApiKey(null, "test", "hash", "prefix",
                "email:send", Instant.now().minusSeconds(1));
        assertFalse(key.isActive(Instant.now()));
    }

    @Test
    void revokedKeyIsNotUsable() {
        ApiKey key = new ApiKey(null, "test", "hash", "prefix",
                "email:send", null);
        key.revoke(Instant.now());
        assertFalse(key.isActive(Instant.now()));
    }

    @Test
    void markUsedUpdatesTimestamp() {
        ApiKey key = new ApiKey(null, "test", "hash", "prefix",
                "email:send", null);
        Instant now = Instant.now();
        key.markUsed(now);
        assertEquals(now, key.getLastUsedAt());
    }
}
