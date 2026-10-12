package com.mailscale.worker.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/**
 * Worker metrics backed by Micrometer with a Prometheus registry.
 *
 * Exposes:
 * - messages consumed / processed / failed
 * - delivery failures by classification (TRANSIENT / PERMANENT)
 * - retry counts
 * - delivery latency with p50/p95/p99 percentiles
 * - batch size distribution for consumer batch tuning
 */
class WorkerMetrics(
    private val registry: PrometheusMeterRegistry =
        PrometheusMeterRegistry(
            PrometheusConfig.DEFAULT
        )
) {

    private val consumedCounter: Counter = Counter
        .builder("mailscale_worker_messages_consumed_total")
        .description("Total messages consumed from Kafka")
        .register(registry)

    private val processedCounter: Counter = Counter
        .builder("mailscale_worker_messages_processed_total")
        .description("Total messages processed successfully")
        .register(registry)

    private val failedCounter: Counter = Counter
        .builder("mailscale_worker_messages_failed_total")
        .description("Total messages that failed delivery")
        .register(registry)

    private val deadLetteredCounter: Counter = Counter
        .builder("mailscale_worker_messages_deadlettered_total")
        .description("Total messages routed to the dead-letter topic")
        .register(registry)

    private val retriedCounter: Counter = Counter
        .builder("mailscale_worker_retries_total")
        .description("Total retry attempts")
        .register(registry)

    private val suppressedCounter: Counter = Counter
        .builder("mailscale_worker_suppressed_total")
        .description("Messages skipped due to suppression")
        .register(registry)

    private val duplicateSkippedCounter: Counter = Counter
        .builder("mailscale_worker_duplicates_skipped_total")
        .description("Duplicate messages detected and skipped")
        .register(registry)

    private val deliveryTimer: Timer = Timer
        .builder("mailscale_worker_delivery_duration_seconds")
        .description("End-to-end processing duration per message")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry)

    private val batchSizeSummary: DistributionSummary = DistributionSummary
        .builder("mailscale_worker_batch_size")
        .description("Kafka poll batch size")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry)

    private val consumerLagGauge: AtomicLong = AtomicLong(0)

    init {
        registry.gauge("mailscale_worker_consumer_lag",
            consumerLagGauge)
    }

    fun recordConsumed() = consumedCounter.increment()

    fun recordProcessed() = processedCounter.increment()

    fun recordFailed() = failedCounter.increment()

    fun recordDeadLettered() = deadLetteredCounter.increment()

    fun recordRetry() = retriedCounter.increment()

    fun recordSuppressed() = suppressedCounter.increment()

    fun recordDuplicateSkipped() = duplicateSkippedCounter.increment()

    fun recordFailureClassification(classification: String) {
        Counter.builder("mailscale_worker_failures_by_class_total")
            .tag("classification", classification)
            .description("Failures classified as TRANSIENT or PERMANENT")
            .register(registry)
            .increment()
    }

    /** Records the end-to-end duration of processing one message. */
    fun recordProcessing(durationMs: Long) {
        deliveryTimer.record(Duration.ofMillis(durationMs))
    }

    /** Records the size of a consumed Kafka batch. */
    fun recordBatch(size: Int) {
        batchSizeSummary.record(size.toDouble())
    }

    fun setConsumerLag(lag: Long) {
        consumerLagGauge.set(lag)
    }

    fun scrape(): String = registry.scrape()
}
