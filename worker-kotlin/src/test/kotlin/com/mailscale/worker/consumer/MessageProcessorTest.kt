package com.mailscale.worker.consumer

import com.mailscale.worker.delivery.DeliveryRecorder
import com.mailscale.worker.delivery.DeliveryRequest
import com.mailscale.worker.delivery.DeliveryResult
import com.mailscale.worker.delivery.EmailProvider
import com.mailscale.worker.delivery.FailureClass
import com.mailscale.worker.metrics.WorkerMetrics
import com.mailscale.worker.model.SendCommand
import com.mailscale.worker.retry.RetryPolicy
import com.mailscale.worker.smtp.MockEmailProvider
import com.mailscale.worker.suppression.SuppressionChecker
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MessageProcessorTest {

    private val suppressionChecker: SuppressionChecker = mockk(relaxed = true)
    private val deliveryRecorder: DeliveryRecorder = mockk(relaxed = true)
    private val metrics = WorkerMetrics(
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    )
    private val retryPolicy = RetryPolicy.deterministic(maxAttempts = 3)

    private lateinit var processor: MessageProcessor

    @BeforeEach
    fun setUp() {
        processor = MessageProcessor(
            provider = MockEmailProvider(),
            suppressionChecker = suppressionChecker,
            deliveryRecorder = deliveryRecorder,
            metrics = metrics,
            retryPolicy = retryPolicy,
            fromAddress = "noreply@mailscale.local"
        )
    }

    private fun command(
        email: String = "user@example.com",
        messageId: String = "11111111-1111-1111-1111-111111111111"
    ) = SendCommand(
        messageId = messageId,
        organizationId = 1L,
        toEmail = email,
        toName = "User",
        subject = "Hello",
        htmlBody = "<p>Hello</p>",
        textBody = "Hello",
        templateId = 1L,
        campaignId = null,
        contactId = null,
        idempotencyKey = null
    )

    @Test
    fun `successful delivery is recorded as SENT`() = runBlocking {
        every { deliveryRecorder.findStatus(any()) } returns "QUEUED"
        every { suppressionChecker.isSuppressed(any(), any()) } returns false
        every { suppressionChecker.consentStatus(any(), any()) } returns "GRANTED"
        every { deliveryRecorder.countAttempts(any()) } returns 0

        val outcome = processor.process(command())

        assertTrue(outcome is MessageProcessor.Outcome.Delivered)
        assertEquals("250",
            (outcome as MessageProcessor.Outcome.Delivered).smtpCode)
    }

    @Test
    fun `suppressed recipient is never sent`() = runBlocking {
        every { deliveryRecorder.findStatus(any()) } returns "QUEUED"
        every { suppressionChecker.isSuppressed(any(), any()) } returns true

        val outcome = processor.process(command(email = "unsub@example.com"))

        assertTrue(outcome is MessageProcessor.Outcome.Suppressed)
        assertEquals(
            "unsub@example.com",
            (outcome as MessageProcessor.Outcome.Suppressed).email
        )
    }

    @Test
    fun `revoked consent is suppressed`() = runBlocking {
        every { deliveryRecorder.findStatus(any()) } returns "QUEUED"
        every { suppressionChecker.isSuppressed(any(), any()) } returns false
        every { suppressionChecker.consentStatus(any(), any()) } returns "REVOKED"

        val outcome = processor.process(command())

        assertTrue(outcome is MessageProcessor.Outcome.Suppressed)
        assertTrue(
            (outcome as MessageProcessor.Outcome.Suppressed)
                .reason.startsWith("consent:")
        )
    }

    @Test
    fun `already terminal message is skipped`() = runBlocking {
        every { deliveryRecorder.findStatus(any()) } returns "DELIVERED"

        val outcome = processor.process(command())

        assertEquals(
            MessageProcessor.Outcome.AlreadyProcessed,
            outcome
        )
    }

    @Test
    fun `transient failure schedules a retry`() = runBlocking {
        val provider = MockEmailProvider(
            transientAddresses = setOf("temp@example.com")
        )
        val localProcessor = MessageProcessor(
            provider, suppressionChecker, deliveryRecorder, metrics,
            retryPolicy, "noreply@mailscale.local"
        )
        every { deliveryRecorder.findStatus(any()) } returns "QUEUED"
        every { suppressionChecker.isSuppressed(any(), any()) } returns false
        every { suppressionChecker.consentStatus(any(), any()) } returns "GRANTED"
        every { deliveryRecorder.countAttempts(any()) } returns 0

        val outcome = localProcessor.process(
            command(email = "temp@example.com")
        )

        assertTrue(outcome is MessageProcessor.Outcome.RetryLater)
        assertEquals(1,
            (outcome as MessageProcessor.Outcome.RetryLater).attempt)
    }

    @Test
    fun `permanent failure stops after max attempts`() = runBlocking {
        val provider = MockEmailProvider(
            failAddresses = setOf("bad@example.com")
        )
        val localProcessor = MessageProcessor(
            provider, suppressionChecker, deliveryRecorder, metrics,
            retryPolicy, "noreply@mailscale.local"
        )
        every { deliveryRecorder.findStatus(any()) } returns "QUEUED"
        every { suppressionChecker.isSuppressed(any(), any()) } returns false
        every { suppressionChecker.consentStatus(any(), any()) } returns "GRANTED"
        every { deliveryRecorder.countAttempts(any()) } returns 3

        val outcome = localProcessor.process(
            command(email = "bad@example.com")
        )

        assertTrue(outcome is MessageProcessor.Outcome.Failed)
        assertEquals(
            FailureClass.PERMANENT,
            (outcome as MessageProcessor.Outcome.Failed).classification
        )
    }

    @Test
    fun `mock provider records sent messages`() = runBlocking {
        val provider = MockEmailProvider(latencyMs = 10)
        val result = provider.send(
            DeliveryRequest(
                toEmail = "x@example.com",
                toName = "X",
                subject = "s",
                htmlBody = "<p>b</p>",
                textBody = "b",
                fromAddress = "f@example.com",
                messageId = "1"
            )
        )
        assertTrue(result is DeliveryResult.Accepted)
        assertEquals(1, provider.sentCount())
        assertEquals(1, provider.sentTo("x@example.com"))
    }
}
