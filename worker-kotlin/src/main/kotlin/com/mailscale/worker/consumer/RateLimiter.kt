package com.mailscale.worker.consumer

/**
 * Bounded rate limiter for provider throttling.
 *
 * <p>Enforces a maximum number of permits per minute using a
 * simple token bucket with nanosecond timestamps. Thread-safe:
 * all state mutations are synchronized so a fixed coroutine
 * dispatcher and the timer thread observe consistent counts.
 */
class RateLimiter(private val permitsPerMinute: Int) {

    private val lock = Any()
    private var windowStart = System.nanoTime()
    private var usedInWindow = 0
    private val windowNanos = 60L * 1_000_000_000L

    /**
     * Acquires a permit, blocking until one is available.
     *
     * @return false if the caller should stop (shutdown requested)
     */
    suspend fun acquire(): Boolean {
        while (true) {
            val waitNanos = synchronized(lock) {
                val now = System.nanoTime()
                if (now - windowStart >= windowNanos) {
                    windowStart = now
                    usedInWindow = 0
                }
                if (usedInWindow < permitsPerMinute) {
                    usedInWindow++
                    return true
                }
                windowNanos - (now - windowStart)
            }
            // Wait for the window to roll over; keeps the coroutine
            // cancellable and avoids blocking a dispatcher thread.
            kotlinx.coroutines.delay(
                waitNanos.coerceAtMost(1_000_000_000L) / 1_000_000
            )
        }
    }

    /** Current utilisation of the current window. */
    fun utilization(): Double = synchronized(lock) {
        usedInWindow.toDouble() / permitsPerMinute
    }
}
