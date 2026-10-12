package com.mailscale.worker.model

/**
 * A single send command consumed from Kafka.
 *
 * Mirrors the payload written by the API's transactional outbox.
 */
@kotlinx.serialization.Serializable
data class SendCommand(
    val messageId: String,
    val organizationId: Long,
    val toEmail: String,
    val toName: String?,
    val subject: String,
    val htmlBody: String?,
    val textBody: String?,
    val templateId: Long?,
    val campaignId: Long?,
    val contactId: Long?,
    val idempotencyKey: String?
)

/** Delivery outcome published back to Kafka for the API. */
@kotlinx.serialization.Serializable
data class DeliveryEvent(
    val messageId: String,
    val organizationId: Long,
    val status: String,
    val provider: String,
    val smtpCode: String?,
    val errorMessage: String?,
    val providerMessageId: String?,
    val attemptNumber: Int,
    val durationMs: Long,
    val suppressed: Boolean = false
)

/** Row shape for the suppression check. */
data class SuppressionHit(val email: String, val reason: String)

/** Message status snapshot used for idempotency checks. */
data class MessageStatusRow(val id: String, val status: String)
