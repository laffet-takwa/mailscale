package com.mailscale.worker.delivery

/** The result of attempting delivery through a provider. */
sealed interface DeliveryResult {

    /** Provider accepted the message. */
    data class Accepted(
        val providerMessageId: String?,
        val smtpCode: Int?,
        val durationMs: Long
    ) : DeliveryResult

    /** Provider rejected the message. */
    data class Rejected(
        val smtpCode: Int?,
        val message: String,
        val classification: FailureClass,
        val exception: Throwable? = null
    ) : DeliveryResult

    /** Delivery was skipped because the recipient is suppressed. */
    data class Suppressed(val email: String, val reason: String) : DeliveryResult
}

/** A request to deliver one email through a provider. */
data class DeliveryRequest(
    val toEmail: String,
    val toName: String?,
    val subject: String,
    val htmlBody: String?,
    val textBody: String?,
    val fromAddress: String,
    val messageId: String
)

/**
 * Pluggable email provider adapter.
 *
 * Implementations: {@link SmtpEmailProvider} for real delivery and
 * {@link MockEmailProvider} for deterministic tests and offline
 * demonstrations.
 */
interface EmailProvider {

    /** Stable identifier used in delivery records and events. */
    val name: String

    /** Attempts delivery. Never throws: failures come back as [DeliveryResult.Rejected]. */
    suspend fun send(request: DeliveryRequest): DeliveryResult

    /** Releases any resources (SMTP sessions). */
    fun close() {}
}
