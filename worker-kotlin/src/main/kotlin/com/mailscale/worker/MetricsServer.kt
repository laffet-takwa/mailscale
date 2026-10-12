package com.mailscale.worker

import com.mailscale.worker.config.WorkerConfig
import com.mailscale.worker.metrics.WorkerMetrics
import com.sun.net.httpserver.HttpServer
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

/**
 * Minimal HTTP server exposing Prometheus metrics and health for
 * the worker (the worker has no Spring Boot, so the JDK's built-in
 * HTTP server keeps the dependency set small).
 */
class MetricsServer(
    private val metrics: WorkerMetrics,
    private val port: Int
) {

    private val log = LoggerFactory.getLogger(MetricsServer::class.java)
    private var server: HttpServer? = null

    fun start() {
        try {
            val http = HttpServer.create(
                InetSocketAddress(port), 0
            )
            http.createContext("/metrics") { exchange ->
                val body = metrics.scrape().toByteArray()
                exchange.responseHeaders.add(
                    "Content-Type", TEXT_PLAIN
                )
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            http.createContext("/health") { exchange ->
                val body = "{\"status\":\"UP\"}".toByteArray()
                exchange.responseHeaders.add(
                    "Content-Type", APPLICATION_JSON
                )
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            http.executor = null
            http.start()
            server = http
            log.info("Metrics/health endpoint started on port {}", port)
        } catch (e: Exception) {
            log.error("Could not start metrics server", e)
        }
    }

    fun stop() {
        server?.stop(0)
    }

    companion object {
        private const val TEXT_PLAIN =
            "text/plain; version=0.0.4; charset=utf-8"
        private const val APPLICATION_JSON = "application/json"
    }
}
