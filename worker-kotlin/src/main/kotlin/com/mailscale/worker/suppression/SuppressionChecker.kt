package com.mailscale.worker.suppression

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

/**
 * Suppression and consent checks for a recipient.
 *
 * <p>Queries the shared PostgreSQL database written by the API
 * tier. Both lookups use the unique index on
 * (organization_id, email), so each check is O(log n).
 */
class SuppressionChecker(private val dataSource: DataSource) {

    /**
     * @return true when the email is on the suppression list for the
     *         organization
     */
    fun isSuppressed(organizationId: Long, email: String): Boolean =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT 1 FROM suppression_entries "
                        + "WHERE organization_id = ? AND email = ? LIMIT 1"
            ).use { statement ->
                statement.setLong(1, organizationId)
                statement.setString(2, email)
                statement.executeQuery().use { it.next() }
            }
        }

    /**
     * @return the status of the contact's consent, or null when the
     *         recipient is not a known contact
     */
    fun consentStatus(organizationId: Long, email: String): String? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT consent_status FROM contacts "
                        + "WHERE organization_id = ? AND email = ?"
            ).use { statement ->
                statement.setLong(1, organizationId)
                statement.setString(2, email)
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.getString(1) else null
                }
            }
        }

    /** Contact status (ACTIVE, UNSUBSCRIBED, BOUNCED) or null. */
    fun contactStatus(organizationId: Long, email: String): String? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT status FROM contacts WHERE organization_id = ? AND email = ?"
            ).use { statement ->
                statement.setLong(1, organizationId)
                statement.setString(2, email)
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.getString(1) else null
                }
            }
        }
}
