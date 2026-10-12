package com.mailscale.worker.retry

import kotlin.random.Random

/**
 * Retry policy combining exponential backoff, a cap, and full
 * jitter to avoid thundering herds.
 *
 * Formula: delay = min(initial * 2^(attempt-1), max) * (1 - jitter)
 * where jitter is uniform in [0, jitterRatio].
 *
 * State transitions between attempts are pure so the policy can
 * be unit-tested without a clock or scheduler.
 */
class RetryPolicy(
    private val maxAttempts: Int = 5,
    private val initialBackoffMs: Long = 2_000,
    private val maxBackoffMs: Long = 60_000,
    private val jitterRatio: Double = 0.2,
    private val random: Random = Random.Default
) {

    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
        require(maxBackoffMs >= initialBackoffMs) { "maxBackoffMs must be >= initialBackoffMs" }
        require(jitterRatio in 0.0..1.0) { "jitterRatio must be in [0,1]" }
    }

    /** The attempt number for a fresh delivery (1-based). */
    fun firstAttempt(): Int = 1

    /** True when another attempt is allowed after [attempt] failures. */
    fun shouldRetry(attempt: Int): Boolean = attempt < maxAttempts

    /** Next attempt number after [attempt]. */
    fun nextAttempt(attempt: Int): Int = attempt + 1

    /**
     * Delay before the next attempt, in milliseconds.
     *
     * @param attempt the attempt number that just failed (1-based)
     */
    fun backoffMs(attempt: Int): Long {
        require(attempt >= 1) { "attempt must be >= 1" }
        // Use a long-based shift to avoid floating-point drift for
        // large attempt numbers.
        val exponential = if (attempt > 30) {
            maxBackoffMs
        } else {
            val doubled = initialBackoffMs shl (attempt - 1).toInt()
            if (doubled < 0L || doubled > maxBackoffMs) maxBackoffMs
            else doubled
        }
        val jitter = if (jitterRatio <= 0.0) 0.0 else random.nextDouble(jitterRatio)
        val factor = 1.0 - jitter
        return (exponential * factor).toLong().coerceAtLeast(1L)
    }

    companion object {
        /** A policy for tests: no jitter, short backoff. */
        fun deterministic(
            maxAttempts: Int = 3,
            initialBackoffMs: Long = 100,
            maxBackoffMs: Long = 400
        ): RetryPolicy = RetryPolicy(
            maxAttempts, initialBackoffMs, maxBackoffMs, 0.0, Random(42)
        )
    }
}
