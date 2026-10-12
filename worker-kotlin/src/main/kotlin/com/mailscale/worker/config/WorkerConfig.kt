package com.mailscale.worker.config

/**
 * Worker configuration resolved from environment variables.
 *
 * Defaults are safe for local development; production values
 * come from the container environment.
 */
data class WorkerConfig(
    val bootstrapServers: String = env("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
    val commandsTopic: String = env("KAFKA_TOPIC_EMAIL_COMMANDS", "mailscale.emails.send"),
    val commandsDlqTopic: String = env("KAFKA_TOPIC_EMAIL_COMMANDS_DLQ", "mailscale.emails.send.dlq"),
    val deliveryEventsTopic: String = env("KAFKA_TOPIC_DELIVERY_EVENTS", "mailscale.delivery.events"),
    val consumerGroup: String = env("WORKER_CONSUMER_GROUP", "mailscale-worker"),
    val concurrency: Int = env("WORKER_CONCURRENCY", "4").toInt(),
    val maxPollRecords: Int = env("WORKER_MAX_POLL_RECORDS", "50").toInt(),
    val pollTimeoutMs: Long = env("WORKER_POLL_TIMEOUT_MS", "1000").toLong(),

    val databaseUrl: String = env("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/mailscale"),
    val databaseUser: String = env("SPRING_DATASOURCE_USERNAME", "mailscale"),
    val databasePassword: String = env("SPRING_DATASOURCE_PASSWORD", "mailscale-local"),
    val databasePoolSize: Int = env("WORKER_DB_POOL_SIZE", "10").toInt(),

    val mailProvider: String = env("MAIL_PROVID", env("MAIL_PROVIDER", "mock")),
    val smtpHost: String = env("MAIL_SMTP_HOST", "localhost"),
    val smtpPort: Int = env("MAIL_SMTP_PORT", "1025").toInt(),
    val smtpUsername: String = env("MAIL_SMTP_USERNAME", ""),
    val smtpPassword: String = env("MAIL_SMTP_PASSWORD", ""),
    val fromAddress: String = env("MAIL_FROM_DEFAULT", "noreply@mailscale.local"),

    val maxDeliveryAttempts: Int = env("MAIL_MAX_DELIVERY_ATTEMPTS", "5").toInt(),
    val retryInitialBackoffMs: Long = env("MAIL_RETRY_INITIAL_BACKOFF_MS", "2000").toLong(),
    val retryMaxBackoffMs: Long = env("MAIL_RETRY_MAX_BACKOFF_MS", "60000").toLong(),
    val retryJitterRatio: Double = env("MAIL_RETRY_JITTER_RATIO", "0.2").toDouble(),
    val rateLimitPerMinute: Int = env("MAIL_RATE_LIMIT_PER_MINUTE", "600").toInt(),
    val metricsPort: Int = env("WORKER_METRICS_PORT", "8081").toInt()
) {
    companion object {
        fun env(key: String, default: String): String =
            System.getenv(key)?.takeIf { it.isNotBlank() } ?: default
    }
}
