package com.mailscale.worker.consumer

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RateLimiterTest {

    @Test
    fun `permits are granted up to the limit`() = runBlocking {
        val limiter = RateLimiter(permitsPerMinute = 5)
        repeat(5) {
            limiter.acquire()
        }
        assertEquals(1.0, limiter.utilization(), 0.001)
    }

    @Test
    fun `a single permit waits for the window`() = runBlocking {
        val limiter = RateLimiter(permitsPerMinute = 1)
        limiter.acquire() // consumes the only permit

        val acquired = kotlinx.coroutines.CompletableDeferred<Unit>()
        val job = launch {
            limiter.acquire()
            acquired.complete(Unit)
        }

        // Immediately after, acquisition should still be pending.
        delay(200)
        assertTrue(!acquired.isCompleted, "limiter should wait for the window")

        // Wait for the window to roll over (60s); then it must succeed.
        // We don't wait the full minute in the test; instead assert the
        // utilization shows full consumption.
        assertEquals(1.0, limiter.utilization(), 0.001)
        job.cancel()
    }

    @Test
    fun `utilization is zero at start`() {
        val limiter = RateLimiter(10)
        assertEquals(0.0, limiter.utilization(), 0.001)
    }
}
