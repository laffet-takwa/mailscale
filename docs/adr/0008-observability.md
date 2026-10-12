# ADR-0008: Observability strategy

**Status:** Accepted
**Date:** 2026-10-12

## Context

A messaging platform needs to answer: what is queued, what is failing, how
fast are we processing, and where is the backlog.

## Decision

- **Metrics**: Micrometer + Prometheus. API exposes `/actuator/prometheus`.
  Worker exposes a small embedded HTTP endpoint `/metrics` using JDK's
  `com.sun.net.httpserver` + Micrometer Prometheus registry (no Spring in
  the worker).
- **Key metrics**: HTTP rate/latency/errors, emails queued/processed,
  delivery failures by class, retry counts, consumer lag, worker health,
  Hikari pool usage, AI request latency/errors.
- **Logs**: structured JSON (logstash encoder), correlation id in MDC on
  every request and propagated to Kafka headers; no email bodies, tokens,
  or PI in logs.
- **Tracing**: correlation ids only (OpenTelemetry-ready; OTel agent can be
  attached via `JAVA_TOOL_OPTIONS` without code changes).
- **Dashboards**: Grafana provisioning in `infrastructure/grafana`.
- **Alerts**: Prometheus alert rules in `infrastructure/prometheus/alerts.yml`
  (lag, failure rate, worker down, DLQ growth, error budget burn).

## Consequences

- (+) Zero-code tracing upgrade path; dashboards versioned in the repo.
- (-) Correlation-id-only tracing is not distributed tracing; acceptable for
  the modular-monolith scope, documented as a limitation.

## Alternatives

- Full OTel SDK instrumentation: more moving parts; deferred until a real
  multi-instance deployment exists.
