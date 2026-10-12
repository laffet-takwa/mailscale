package com.mailscale.worker

import com.mailscale.worker.config.WorkerConfig
import com.mailscale.worker.consumer.EmailWorker
import com.mailscale.worker.delivery.DeliveryRecorder
import com.mailscale.worker.metrics.WorkerMetrics
import com.mailscale.worker.smtp.MockEmailProvider
import com.mailscale.worker.smtp.SmtpEmailProvider
import com.mailscale.worker.suppression.SuppressionChecker
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory

fun main() {
    val log = LoggerFactory.getLogger("com.mailscale.worker.Main")
    val config = WorkerConfig()
    val metrics = WorkerMetrics()

    val dataSource = createDataSource(config)
    val provider = createProvider(config)

    val worker = EmailWorker(
        config = config,
        provider = provider,
        suppressionChecker = SuppressionChecker(dataSource),
        deliveryRecorder = DeliveryRecorder(dataSource),
        metrics = metrics,
        objectMapper = ObjectMapper()
    )

    MetricsServer(metrics, config.metricsPort).start()
    log.info("Metrics endpoint on :{}", config.metricsPort)

    worker.start()

    dataSource.close()
    log.info("Worker process exiting")
}

private fun createDataSource(config: WorkerConfig): HikariDataSource {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.databaseUrl
        username = config.databaseUser
        password = config.databasePassword
        maximumPoolSize = config.databasePoolSize
        minimumIdle = 2
        connectionTimeout = 30_000
        idleTimeout = 600_000
        maxLifetime = 1_800_000
        poolName = "mailscale-worker"
    }
    return HikariDataSource(hikariConfig)
}

private fun createProvider(config: WorkerConfig) =
    when (config.mailProvider.lowercase()) {
        "smtp" -> SmtpEmailProvider(
            host = config.smtpHost,
            port = config.smtpPort,
            username = config.smtpUsername.ifBlank { null },
            password = config.smtpPassword.ifBlank { null },
            fromAddress = config.fromAddress
        )

        "mock" -> {
            println(
                "MailScale worker running with MOCK provider - "
                        + "no real email is sent"
            )
            MockEmailProvider()
        }

        else -> throw IllegalArgumentException(
            "Unknown MAIL_PROVID: ${config.mailProvider}"
        )
    }
