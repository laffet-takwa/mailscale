package com.mailscale.worker.smtp

import com.mailscale.worker.delivery.DeliveryRequest
import com.mailscale.worker.delivery.DeliveryResult
import com.mailscale.worker.delivery.EmailProvider
import com.mailscale.worker.delivery.SmtpErrorClassifier
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.slf4j.LoggerFactory
import java.util.Properties
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SMTP adapter implementing [EmailProvider].
 *
 * <p>Connects to any SMTP server (Mailpit locally, a real provider
 * when configured). Sends multipart/alternative messages with an
 * HTML part and a text part.
 *
 * <p>Blocking transport work is moved to [Dispatchers.IO] so the
 * coroutine dispatcher stays responsive; the caller's structured
 * concurrency still governs the lifecycle.
 */
class SmtpEmailProvider(
    private val host: String,
    private val port: Int,
    private val username: String?,
    private val password: String?,
    private val fromAddress: String,
    private val session: Session = buildSession(
        host, port, username, password
    )
) : EmailProvider {

    override val name: String = "smtp"

    override suspend fun send(request: DeliveryRequest): DeliveryResult =
        withContext(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            try {
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(fromAddress))
                    setRecipient(
                        Message.RecipientType.TO,
                        InternetAddress(request.toEmail, request.toName ?: "")
                    )
                    subject = request.subject

                    if (request.htmlBody != null && request.textBody != null) {
                        val htmlPart = MimeBodyPart().apply {
                            setContent(request.htmlBody, "text/html; charset=utf-8")
                        }
                        val textPart = MimeBodyPart().apply {
                            setText(request.textBody, "utf-8")
                        }
                        setContent(
                            MimeMultipart("alternative").apply {
                                addBodyPart(textPart)
                                addBodyPart(htmlPart)
                            }
                        )
                    } else if (request.htmlBody != null) {
                        setContent(request.htmlBody, "text/html; charset=utf-8")
                    } else {
                        setText(request.textBody ?: "", "utf-8")
                    }

                    setHeader("X-MailScale-Message-Id", request.messageId)
                }

                val messageId = suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation {
                        log.debug("SMTP send cancelled for {}", request.messageId)
                    }
                    try {
                        Transport.send(message)
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }
                }

                DeliveryResult.Accepted(
                    providerMessageId = messageId,
                    smtpCode = 250,
                    durationMs = System.currentTimeMillis() - started
                )
            } catch (e: Exception) {
                val code = extractSmtpCode(e)
                DeliveryResult.Rejected(
                    smtpCode = code,
                    message = e.message ?: e.javaClass.simpleName,
                    classification = SmtpErrorClassifier.classify(
                        code, e.message, e
                    ),
                    exception = e
                )
            }
        }

    /**
     * Extracts an SMTP reply code from an exception where the API
     * exposes it, otherwise from the message text (e.g. "550 ...").
     */
    private fun extractSmtpCode(exception: Throwable): Int? {
        val message = exception.message ?: return null
        Regex("\\b([45]\\d\\d)\\b").find(message)?.let {
            return it.groupValues[1].toInt()
        }
        return when (exception) {
            is jakarta.mail.MessagingException ->
                exception.nextException?.let { extractSmtpCode(it) }
            else -> null
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(SmtpEmailProvider::class.java)

        fun buildSession(
            host: String,
            port: Int,
            username: String?,
            password: String?
        ): Session {
            val props = Properties().apply {
                put("mail.smtp.host", host)
                put("mail.smtp.port", port.toString())
                put("mail.smtp.auth", (!username.isNullOrBlank()).toString())
                put("mail.smtp.starttls.enable", "false")
                put("mail.smtp.timeout", "30000")
                put("mail.smtp.connectiontimeout", "10000")
                put("mail.smtp.writetimeout", "30000")
            }
            val authenticator = if (!username.isNullOrBlank()) {
                object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication =
                        PasswordAuthentication(username, password ?: "")
                }
            } else {
                null
            }
            return Session.getInstance(props, authenticator)
        }
    }
}
