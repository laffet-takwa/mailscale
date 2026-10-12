package com.mailscale.worker.retry

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RetryPolicyTest {

    @Test
    fun `respects max attempts`() {
        val policy = RetryPolicy(maxAttempts = 3)
        assertTrue(policy.shouldRetry(1))
        assertTrue(policy.shouldRetry(2))
        assertFalse(policy.shouldRetry(3))
        assertFalse(policy.shouldRetry(4))
    }

    @Test
    fun `backoff is exponential and capped without jitter`() {
        val policy = RetryPolicy(
            maxAttempts = 10,
            initialBackoffMs = 1_000,
            maxBackoffMs = 30_000,
            jitterRatio = 0.0,
            random = kotlin.random.Random(1)
        )
        assertEquals(1_000, policy.backoffMs(1))
        assertEquals(2_000, policy.backoffMs(2))
        assertEquals(4_000, policy.backoffMs(3))
        assertEquals(8_000, policy.backoffMs(4))
        assertEquals(16_000, policy.backoffMs(5))
        // capped at 30s from attempt 6 onwards
        assertEquals(30_000, policy.backoffMs(6))
        assertEquals(30_000, policy.backoffMs(20))
    }

    @Test
    fun `backoff with jitter stays within bounds`() {
        val policy = RetryPolicy(
            maxAttempts = 5,
            initialBackoffMs = 1_000,
            maxBackoffMs = 10_000,
            jitterRatio = 0.2,
            random = kotlin.random.Random(7)
        )
        for (attempt in 1..4) {
            val base = 1_000L * (1L shl (attempt - 1))
            val delay = policy.backoffMs(attempt)
            assertTrue(
                delay >= (base * 0.8).toLong(),
                "delay $delay below lower bound for attempt $attempt"
            )
            assertTrue(
                delay <= base,
                "delay $delay above base for attempt $attempt"
            )
        }
    }

    @Test
    fun `backoff never returns non-positive value`() {
        val policy = RetryPolicy(
            maxAttempts = 2,
            initialBackoffMs = 1,
            maxBackoffMs = 2,
            jitterRatio = 1.0
        )
        for (i in 1..10) {
            assertTrue(policy.backoffMs(1) >= 1)
        }
    }

    @Test
    fun `attempt numbers advance by one`() {
        val policy = RetryPolicy(maxAttempts = 5)
        assertEquals(1, policy.firstAttempt())
        assertEquals(2, policy.nextAttempt(1))
        assertEquals(5, policy.nextAttempt(4))
    }

    @Test
    fun `invalid configuration is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            RetryPolicy(maxAttempts = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RetryPolicy(initialBackoffMs = 100, maxBackoffMs = 50)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RetryPolicy(jitterRatio = 1.5)
        }
    }
}
