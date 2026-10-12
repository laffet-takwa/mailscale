package com.mailscale.worker.delivery

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SmtpErrorClassifierTest {

    @Test
    fun `4xx codes are transient`() {
        assertEquals(FailureClass.TRANSIENT, SmtpErrorClassifier.classify(421, "Try later"))
        assertEquals(FailureClass.TRANSIENT, SmtpErrorClassifier.classify(451, "Greylisted"))
        assertEquals(FailureClass.TRANSIENT, SmtpErrorClassifier.classify(450, "Busy"))
    }

    @Test
    fun `5xx codes are permanent`() {
        assertEquals(FailureClass.PERMANENT, SmtpErrorClassifier.classify(550, "No such user"))
        assertEquals(FailureClass.PERMANENT, SmtpErrorClassifier.classify(552, "Too big"))
        assertEquals(FailureClass.PERMANENT, SmtpErrorClassifier.classify(554, "Spam blocked"))
    }

    @Test
    fun `network timeouts are transient`() {
        assertEquals(
            FailureClass.TRANSIENT,
            SmtpErrorClassifier.classify(
                null,
                null,
                java.net.SocketTimeoutException("read timed out")
            )
        )
        assertEquals(
            FailureClass.TRANSIENT,
            SmtpErrorClassifier.classify(
                null,
                null,
                java.net.ConnectException("refused")
            )
        )
    }

    @Test
    fun `io errors are transient`() {
        assertEquals(
            FailureClass.TRANSIENT,
            SmtpErrorClassifier.classify(
                null, null, java.io.IOException("broken pipe")
            )
        )
    }

    @Test
    fun `unknown exceptions are permanent`() {
        assertEquals(
            FailureClass.PERMANENT,
            SmtpErrorClassifier.classify(
                null, null, IllegalStateException("bad config")
            )
        )
    }

    @Test
    fun `words classify when no code or exception`() {
        assertEquals(
            FailureClass.TRANSIENT,
            SmtpErrorClassifier.classify(null, "service temporarily unavailable", null)
        )
        assertEquals(
            FailureClass.PERMANENT,
            SmtpErrorClassifier.classify(null, "unknown recipient", null)
        )
        assertEquals(
            FailureClass.PERMANENT,
            SmtpErrorClassifier.classify(null, "invalid address", null)
        )
    }

    @Test
    fun `no information defaults to permanent`() {
        assertEquals(
            FailureClass.PERMANENT,
            SmtpErrorClassifier.classify(null, null, null)
        )
    }

    @Test
    fun `address validation`() {
        assertTrue(SmtpErrorClassifier.isValidEmail("user@example.com"))
        assertTrue(SmtpErrorClassifier.isValidEmail("first.last+tag@sub.example.co"))
        assertTrue(SmtpErrorClassifier.isValidEmail("user_name@example-domain.org"))
        assertTrue(!SmtpErrorClassifier.isValidEmail("not-an-email"))
        assertTrue(!SmtpErrorClassifier.isValidEmail("user@"))
        assertTrue(!SmtpErrorClassifier.isValidEmail("@example.com"))
        assertTrue(!SmtpErrorClassifier.isValidEmail("user@example"))
        assertTrue(!SmtpErrorClassifier.isValidEmail(""))
    }
}
