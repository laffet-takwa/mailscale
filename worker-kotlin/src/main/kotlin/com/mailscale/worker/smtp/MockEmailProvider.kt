package com.mailscale.worker.smtp

import com.mailscale.worker.delivery.DeliveryRequest
import com.mailscale.worker.delivery.DeliveryResult
import com.mailscale.worker.delivery.EmailProvider

/**
 * Deterministic in-memory provider used for offline demonstrations
 * and tests. Never performs network I/O.
 *
 * <p>Addresses equal to [failAddresses] are rejected as permanent
 * failures; addresses equal to [transientAddresses] are rejected as
 * transient failures so retry logic can be exercised.
 */
class MockEmailProvider(
    private val latencyMs: Long = 0,
    private val transientAddresses: Set<String> = emptySet(),
    private val failAddresses: Set<String> = emptySet()
) : EmailProvider {

    override val name: String = "mock"

    val sent: MutableList<DeliveryRequest> = mutableListOf()
    private val lock = Any()

    override suspend fun send(request: DeliveryRequest): DeliveryResult {
        if (latencyMs > 0) {
            kotlinx.coroutines.delay(latencyMs)
        }
        synchronized(lock) { sent.add(request) }

        return when {
            request.toEmail.lowercase() in transientAddresses ->
                DeliveryResult.Rejected(
                    smtpCode = 451,
                    message = "Temporary failure for ${request.toEmail}",
                    classification =
                        com.mailscale.worker.delivery.FailureClass.TRANSIENT
                )

            request.toEmail.lowercase() in failAddresses ->
                DeliveryResult.Rejected(
                    smtpCode = 550,
                    message = "Mailbox unavailable for ${request.toEmail}",
                    classification =
                        com.mailscale.worker.delivery.FailureClass.PERMANENT
                )

            else -> DeliveryResult.Accepted(
                providerMessageId = "mock-${request.messageId}",
                smtpCode = 250,
                durationMs = latencyMs
            )
        }
    }

    fun sentCount(): Int = synchronized(lock) { sent.size }

    fun sentTo(email: String): Int = synchronized(lock) {
        sent.count { it.toEmail.equals(email, ignoreCase = true) }
    }
}
