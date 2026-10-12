package com.mailscale.worker.consumer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.mailscale.worker.config.WorkerConfig
import com.mailscale.worker.delivery.DeliveryRecorder
import com.mailscale.worker.delivery.EmailProvider
import com.mailscale.worker.metrics.WorkerMetrics
import com.mailscale.worker.model.SendCommand
import com.mailscale.worker.retry.RetryPolicy
import com.mailscale.worker.suppression.SuppressionChecker
import java.time.Duration
import java.util.Collections
import java.util.Properties
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory

/**
 * Bounded, single-thread-poll consumer that dispatches message
 * processing to a fixed coroutine dispatcher.
 *
 * <p>Concurrency model (ADR-0001):
 * <ul>
 *   <li>One poll thread (Kafka requirement) subscribed to the
 *       command topic</li>
 *   <li>A fixed thread pool of size `concurrency` backing a
 *       coroutine dispatcher - no unbounded thread creation</li>
 *   <li>Offsets committed only after the batch's work completes,
 *       giving at-least-once processing</li>
 * </ul>
 *
 * <p>Delayed retries: transient failures are re-published to the
 * same topic after the computed backoff, so no thread blocks waiting.
 * Permissions are checked with [RateLimiter] before delivery.
 */
class EmailWorker(
    private val config: WorkerConfig,
    private val provider: EmailProvider,
    private val suppressionChecker: SuppressionChecker,
    private val deliveryRecorder: DeliveryRecorder,
    private val metrics: WorkerMetrics,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(EmailWorker::class.java)

    private val running = AtomicBoolean(false)
    private val shutdownLatch = CountDownLatch(1)

    private val retryPolicy = RetryPolicy(
        maxAttempts = config.maxDeliveryAttempts,
        initialBackoffMs = config.retryInitialBackoffMs,
        maxBackoffMs = config.retryMaxBackoffMs,
        jitterRatio = config.retryJitterRatio
    )

    private val consumer: KafkaConsumer<String, String> by lazy {
        KafkaConsumer<String, String>(consumerProps()).apply {
            subscribe(listOf(config.commandsTopic))
        }
    }

    private val producer: KafkaProducer<String, String> by lazy {
        KafkaProducer<String, String>(producerProps())
    }

    private val executor = Executors.newFixedThreadPool(
        config.concurrency,
        object : ThreadFactory {
            private val counter = java.util.concurrent.atomic.AtomicInteger()
            override fun newThread(r: Runnable): Thread =
                Thread(r, "mailscale-worker-${counter.incrementAndGet()}")
                    .apply { isDaemon = false }
        }
    )
    private val dispatcher = executor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val rateLimiter = RateLimiter(config.rateLimitPerMinute)

    private val processor = MessageProcessor(
        provider = provider,
        suppressionChecker = suppressionChecker,
        deliveryRecorder = deliveryRecorder,
        metrics = metrics,
        retryPolicy = retryPolicy,
        fromAddress = config.fromAddress
    )

    /** Starts the polling loop; blocks until shutdown is requested. */
    fun start() {
        if (running.compareAndSet(false, true).not()) {
            log.warn("Worker already running")
            return
        }
        Runtime.getRuntime().addShutdownHook(
            Thread({ stop() }, "worker-shutdown")
        )
        log.info(
            "MailScale worker starting: brokers={} topic={} group={} "
                    + "concurrency={} provider={}",
            config.bootstrapServers, config.commandsTopic,
            config.consumerGroup, config.concurrency, provider.name
        )
        try {
            runBlocking { pollLoop() }
        } finally {
            shutdownLatch.countDown()
        }
    }

    /** Requests a graceful shutdown. */
    fun stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Worker shutdown requested")
        }
        try {
            consumer.wakeup()
        } catch (_: Exception) {
            // consumer may not be initialised
        }
    }

    private suspend fun pollLoop() {
        while (running.get()) {
            val records: ConsumerRecords<String, String> = try {
                consumer.poll(Duration.ofMillis(config.pollTimeoutMs))
            } catch (e: WakeupException) {
                if (running.get()) {
                    log.warn("Consumer wakeup while still running", e)
                    continue
                }
                break
            } catch (e: Exception) {
                log.error("Poll failed, retrying in 1s", e)
                delay(1_000)
                continue
            }

            if (records.isEmpty) {
                continue
            }
            metrics.recordBatch(records.count())

            var processed = 0
            val jobs = Collections.synchronizedList(mutableListOf<kotlinx.coroutines.Job>())
            for (record in records) {
                if (!running.get()) {
                    break
                }
                jobs.add(
                    scope.launch {
                        processRecord(record)
                    }
                )
            }
            jobs.forEach { it.join() }
            processed = jobs.size

            try {
                consumer.commitSync()
            } catch (e: Exception) {
                log.error("Commit failed", e)
            }
            log.debug("Batch of {} committed", processed)
        }

        close()
    }

    private suspend fun processRecord(record: ConsumerRecord<String, String>) {
        val command = try {
            parseCommand(record.value())
        } catch (e: Exception) {
            log.error(
                "Malformed command at offset {}: {}",
                record.offset(), e.message
            )
            publishToDlq(record.value(), "MALFORMED", e.message)
            return
        }

        metrics.recordConsumed()

        // Provider rate limiting (backpressure before delivery).
        rateLimiter.acquire()

        val outcome = withTimeoutOrNull(DELIVERY_TIMEOUT_MS) {
            processor.process(command)
        } ?: run {
            log.warn("Delivery timed out for {}", command.messageId)
            publishToDlq(
                record.value(), "TIMEOUT", "Processing exceeded "
                        + "${DELIVERY_TIMEOUT_MS}ms"
            )
            return
        }

        when (outcome) {
            is MessageProcessor.Outcome.Delivered -> {
                publishDeliveryEvent(command, "SENT",
                    smtpCode = outcome.smtpCode, errorMessage = null)
            }

            is MessageProcessor.Outcome.Suppressed -> {
                publishDeliveryEvent(command, "SUPPRESSED",
                    errorMessage = outcome.reason)
                log.debug("Suppressed {} for message {}",
                    outcome.email, command.messageId)
            }

            is MessageProcessor.Outcome.AlreadyProcessed -> {
                // Nothing to do: idempotent no-op.
            }

            is MessageProcessor.Outcome.RetryLater -> {
                metrics.recordRetry()
                val delayMs = retryPolicy.backoffMs(outcome.attempt)
                delay(delayMs)
                if (!running.get()) {
                    return
                }
                log.info(
                    "Retrying message {} (attempt {}) in {}ms",
                    command.messageId,
                    retryPolicy.nextAttempt(outcome.attempt),
                    delayMs
                )
                publishRetry(record)
            }

            is MessageProcessor.Outcome.Failed -> {
                publishDeliveryEvent(command, "FAILED",
                    errorMessage = "${outcome.classification}")
                metrics.recordDeadLettered()
                publishToDlq(
                    record.value(),
                    outcome.classification.name,
                    "Max attempts reached"
                )
            }
        }
    }

    private fun parseCommand(payload: String): SendCommand {
        val json = objectMapper.readValue(payload, JsonNode::class.java)
        return SendCommand(
            messageId = json.reqText("messageId"),
            organizationId = json.reqLong("organizationId"),
            toEmail = json.reqText("toEmail"),
            toName = json.optText("toName"),
            subject = json.reqText("subject"),
            htmlBody = json.optText("htmlBody"),
            textBody = json.optText("textBody"),
            templateId = json.optLong("templateId"),
            campaignId = json.optLong("campaignId"),
            contactId = json.optLong("contactId"),
            idempotencyKey = json.optText("idempotencyKey")
        )
    }

    private fun JsonNode.reqText(field: String): String =
        if (hasNonNull(field)) get(field).asText() else throw
            IllegalArgumentException("Missing field: $field")

    private fun JsonNode.reqLong(field: String): Long =
        if (hasNonNull(field)) get(field).asLong() else throw
            IllegalArgumentException("Missing field: $field")

    private fun JsonNode.optText(field: String): String? =
        if (hasNonNull(field)) get(field).asText() else null

    private fun JsonNode.optLong(field: String): Long? =
        if (hasNonNull(field)) get(field).asLong() else null
    private fun publishDeliveryEvent(
        command: SendCommand,
        status: String,
        smtpCode: String? = null,
        errorMessage: String? = null
    ) {
        val event = mapOf(
            "messageId" to command.messageId,
            "organizationId" to command.organizationId,
            "status" to status,
            "provider" to provider.name,
            "smtpCode" to smtpCode,
            "errorMessage" to errorMessage
        )
        try {
            producer.send(
                ProducerRecord(
                    config.deliveryEventsTopic,
                    command.messageId,
                    objectMapper.writeValueAsString(event)
                )
            )
        } catch (e: Exception) {
            log.error("Failed to publish delivery event for {}: {}",
                command.messageId, e.message)
        }
    }

    /** Re-publishes a record for another attempt. */
    private fun publishRetry(record: ConsumerRecord<String, String>) {
        try {
            producer.send(
                ProducerRecord(record.topic(), record.key(), record.value())
            )
        } catch (e: Exception) {
            log.error("Failed to re-publish retry", e)
        }
    }

    private fun publishToDlq(payload: String, reason: String, detail: String?) {
        try {
            val record = ProducerRecord<String, String>(
                config.commandsDlqTopic, null, payload
            )
            record.headers().add("errorCode", reason.toByteArray())
            if (detail != null) {
                record.headers().add("lastError", detail.toByteArray())
            }
            record.headers().add(
                "workerTimestamp",
                System.currentTimeMillis().toString().toByteArray()
            )
            producer.send(record)
        } catch (e: Exception) {
            log.error("Failed to publish to DLQ", e)
        }
    }

    private fun consumerProps(): Properties = Properties().apply {
        put("bootstrap.servers", config.bootstrapServers)
        put("group.id", config.consumerGroup)
        put("enable.auto.commit", "false")
        put("auto.offset.reset", "earliest")
        put("max.poll.records", config.maxPollRecords.toString())
        put("max.poll.interval.ms", "300000")
        put("session.timeout.ms", "30000")
        put("key.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer")
        put("value.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer")
    }

    private fun producerProps(): Properties = Properties().apply {
        put("bootstrap.servers", config.bootstrapServers)
        put("acks", "all")
        put("enable.idempotence", "true")
        put("max.in.flight.requests.per.connection", "5")
        put("key.serializer",
            "org.apache.kafka.common.serialization.StringSerializer")
        put("value.serializer",
            "org.apache.kafka.common.serialization.StringSerializer")
    }

    private fun close() {
        log.info("Closing worker resources")
        try {
            producer.close(Duration.ofSeconds(5))
        } catch (e: Exception) {
            log.warn("Producer close failed", e)
        }
        try {
            consumer.close(Duration.ofSeconds(5))
        } catch (e: Exception) {
            log.warn("Consumer close failed", e)
        }
        dispatcher.close()
        provider.close()
        log.info("Worker stopped")
    }

    private companion object {
        const val DELIVERY_TIMEOUT_MS = 30_000L
    }
}
