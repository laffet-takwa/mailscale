package com.mailscale.worker.delivery

/** Classification of a delivery failure. */
enum class FailureClass {
    /** Retry later: SMTP 4xx, timeouts, connection resets. */
    TRANSIENT,

    /** Do not retry: SMTP 5xx, invalid recipient address. */
    PERMANENT
}

/**
 * Classifies SMTP errors as transient or permanent.
 *
 * <p>Rules (documented in docs/architecture/overview.md):
 * <ul>
 *   <li>SMTP 4xx -> TRANSIENT (mailbox busy, try later)</li>
 *   <li>SMTP 5xx -> PERMANENT (mailbox unknown, rejected)</li>
 *   <li>SMTP 552, 554 with "recipient" in message -> PERMANENT</li>
 *   <li>Connection/IO/timeout exceptions -> TRANSIENT</li>
 *   <li>Invalid address format -> PERMANENT</li>
 * </ul>
 */
object SmtpErrorClassifier {

    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    fun classify(smtpCode: Int?, message: String?, exception: Throwable? = null): FailureClass {
        if (smtpCode != null) {
            return when {
                smtpCode in 400..499 -> FailureClass.TRANSIENT
                smtpCode >= 500 -> FailureClass.PERMANENT
                else -> FailureClass.PERMANENT
            }
        }
        if (exception != null) {
            return when (exception) {
                is java.net.SocketTimeoutException -> FailureClass.TRANSIENT
                is java.net.ConnectException -> FailureClass.TRANSIENT
                is java.io.IOException -> FailureClass.TRANSIENT
                else -> FailureClass.PERMANENT
            }
        }
        if (message != null) {
            val lower = message.lowercase()
            return when {
                lower.contains("timeout") -> FailureClass.TRANSIENT
                lower.contains("try again") -> FailureClass.TRANSIENT
                lower.contains("temporarily") -> FailureClass.TRANSIENT
                lower.contains("invalid address") -> FailureClass.PERMANENT
                lower.contains("does not exist") -> FailureClass.PERMANENT
                lower.contains("unknown recipient") -> FailureClass.PERMANENT
                else -> FailureClass.PERMANENT
            }
        }
        return FailureClass.PERMANENT
    }

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email)
}
