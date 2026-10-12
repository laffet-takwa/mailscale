package com.mailscale.worker.delivery

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/**
 * Persists delivery state written by the worker.
 *
 * <p>All statements are parameterised (no string concatenation),
 * which prevents SQL injection. Batched inserts reduce round trips
 * for campaign batches.
 */
class DeliveryRecorder(private val dataSource: DataSource) {

    /** Current status of a message, or null when unknown. */
    fun findStatus(messageId: UUID): String? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT status FROM email_messages WHERE id = ?"
            ).use { statement ->
                statement.setObject(1, messageId)
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.getString(1) else null
                }
            }
        }

    /** Records one attempt and updates the message row. */
    fun recordDelivery(
        messageId: UUID,
        attemptNumber: Int,
        provider: String,
        status: String,
        smtpCode: String?,
        errorMessage: String?,
        durationMs: Long,
        providerMessageId: String?
    ) {
        dataSource.connection.use { connection ->
            val autoCommit = connection.autoCommit
            connection.autoCommit = false
            try {
                connection.prepareStatement(
                    "INSERT INTO delivery_attempts "
                            + "(message_id, attempt_number, provider, status, "
                            + "smtp_code, error_message, duration_ms) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)"
                ).use { insert ->
                    insert.setObject(1, messageId)
                    insert.setInt(2, attemptNumber)
                    insert.setString(3, provider)
                    insert.setString(4, status)
                    insert.setString(5, smtpCode)
                    insert.setString(6, errorMessage)
                    insert.setLong(7, durationMs)
                    insert.executeUpdate()
                }
                connection.prepareStatement(
                    "UPDATE email_messages SET status = ?, provider_message_id = ?, "
                            + "updated_at = ? WHERE id = ?"
                ).use { update ->
                    update.setString(1, status)
                    update.setString(2, providerMessageId)
                    update.setTimestamp(3, Timestamp.from(Instant.now()))
                    update.setObject(4, messageId)
                    update.executeUpdate()
                }
                connection.commit()
            } catch (e: SQLException) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = autoCommit
            }
        }
    }

    /** Marks a message SUPPRESSED (no delivery attempted). */
    fun recordSuppressed(messageId: UUID, provider: String) {
        recordDelivery(messageId, 1, provider, "SUPPRESSED",
            null, "suppressed", 0, null)
    }

    /** Counts prior attempts for a message (used for retry accounting). */
    fun countAttempts(messageId: UUID): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT COUNT(*) FROM delivery_attempts WHERE message_id = ?"
            ).use { statement ->
                statement.setObject(1, messageId)
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
}
