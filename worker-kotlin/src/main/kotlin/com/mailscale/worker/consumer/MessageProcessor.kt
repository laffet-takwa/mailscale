package com.mailscale.worker.consumer

import com.mailscale.worker.delivery.DeliveryRecorder
import com.mailscale.worker.delivery.DeliveryRequest
import com.mailscale.worker.delivery.DeliveryResult
import com.mailscale.worker.delivery.EmailProvider
import com.mailscale.worker.delivery.FailureClass
import com.mailscale.worker.metrics.WorkerMetrics
import com.mailscale.worker.model.SendCommand
import com.mailscale.worker.retry.RetryPolicy
import com.mailscale.worker.suppression.SuppressionChecker
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * Processes one send command: suppression checks, provider delivery,
 * retry decision, and delivery-state persistence.
 *
 * <p>Idempotency: a message whose status is already terminal
 * (DELIVERED, FAILED, SUPPRESSED) is skipped, so Kafka redelivery
 * cannot send twice.
 */
class MessageProcessor(
    private val provider: EmailProvider,
    private val suppressionChecker: SuppressionChecker,
    private val deliveryRecorder: DeliveryRecorder,
    private val metrics: WorkerMetrics,
    private val retryPolicy: RetryPolicy,
    private val fromAddress: String
) {

    private val log = LoggerFactory.getLogger(MessageProcessor::class.java)

    /** Outcome of processing a single command. */
    sealed interface Outcome {
        data class Delivered(val smtpCode: String?) : Outcome

        data class Suppressed(val email: String, val reason: String) : Outcome

        data class RetryLater(val attempt: Int) : Outcome

        data class Failed(val classification: FailureClass) : Outcome

        /** Message was already terminal; skipped for idempotency. */
        data object AlreadyProcessed : Outcome
    }

    suspend fun process(command: SendCommand): Outcome {
        val started = System.currentTimeMillis()
        val messageId = UUID.fromString(command.messageId)

        // Idempotency guard: terminal statuses are never re-delivered.
        when (deliveryRecorder.findStatus(messageId)) {
            "DELIVERED", "FAILED", "SUPPRESSED" -> {
                metrics.recordDuplicateSkipped()
                log.debug("Skipping already-processed message {}", messageId)
                return Outcome.AlreadyProcessed
            }
            else -> Unit
        }

        // Suppression check before any provider call.
        if (suppressionChecker.isSuppressed(
                command.organizationId, command.toEmail
            )
        ) {
            deliveryRecorder.recordSuppressed(messageId, provider.name)
            metrics.recordSuppressed()
            metrics.recordProcessing(System.currentTimeMillis() - started)
            return Outcome.Suppressed(command.toEmail, "suppressed")
        }

        // Consent check for known contacts.
        val consent = suppressionChecker.consentStatus(
            command.organizationId, command.toEmail
        )
        if (consent != null && consent != "GRANTED") {
            deliveryRecorder.recordSuppressed(messageId, provider.name)
            metrics.recordSuppressed()
            metrics.recordProcessing(System.currentTimeMillis() - started)
            return Outcome.Suppressed(command.toEmail, "consent:$consent")
        }

        val attempt = deliveryRecorder.countAttempts(messageId) + 1
        val result = provider.send(
            DeliveryRequest(
                toEmail = command.toEmail,
                toName = command.toName,
                subject = command.subject,
                htmlBody = command.htmlBody,
                textBody = command.textBody,
                fromAddress = fromAddress,
                messageId = command.messageId
            )
        )

        return when (result) {
            is DeliveryResult.Accepted -> {
                deliveryRecorder.recordDelivery(
                    messageId, attempt, provider.name, "SENT",
                    result.smtpCode?.toString(), null,
                    result.durationMs, result.providerMessageId
                )
                metrics.recordProcessed()
                metrics.recordProcessing(System.currentTimeMillis() - started)
                Outcome.Delivered(result.smtpCode?.toString())
            }

            is DeliveryResult.Suppressed -> {
                deliveryRecorder.recordSuppressed(messageId, provider.name)
                metrics.recordSuppressed()
                metrics.recordProcessing(System.currentTimeMillis() - started)
                Outcome.Suppressed(result.email, "provider-suppressed")
            }

            is DeliveryResult.Rejected -> {
                deliveryRecorder.recordDelivery(
                    messageId, attempt, provider.name, "FAILED",
                    result.smtpCode?.toString(), result.message,
                    0L, null
                )
                metrics.recordFailed()
                metrics.recordFailureClassification(
                    result.classification.name
                )
                if (result.classification == FailureClass.TRANSIENT
                    && retryPolicy.shouldRetry(attempt)
                ) {
                    metrics.recordRetry()
                    Outcome.RetryLater(attempt)
                } else {
                    metrics.recordProcessing(
                        System.currentTimeMillis() - started
                    )
                    Outcome.Failed(result.classification)
                }
            }
        }
    }
}
