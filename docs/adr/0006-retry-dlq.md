# ADR-0006: Retry and dead-letter policy

**Status:** Accepted
**Date:** 2026-10-12

## Context

Delivery failures are either transient (SMTP 4xx, timeouts) or permanent
(invalid address, suppressed recipient). Retrying permanent failures wastes
provider quota; not retrying transient failures loses email.

## Decision

- **Classification**: `SmtpCodeClassifier` maps SMTP reply codes and
  exceptions to `TRANSIENT` or `PERMANENT`:
  - 4xx and network timeouts -> transient
  - 5xx (except 5xx to a valid recipient that indicates policy) and
    malformed addresses -> permanent
- **Backoff**: exponential with full jitter,
  `delay = min(initial * 2^(n-1), max) * (1 - jitterRatio..1)`,
  default initial 2s, max 60s, jitter 20%.
- **Retry accounting**: attempts are persisted per message; the worker uses a
  delayed-retry queue (delayed via re-publish with `retryAt` header, consumed
  after the delay by the same topic's delayed consumer) to avoid blocking
  consumer threads.
- **Dead-letter**: after `MAIL_MAX_DELIVERY_ATTEMPTS` transient failures the
  record is published to the DLQ with diagnostic headers and the message is
  marked `FAILED`.
- **Permanent failure**: no retry, message marked `FAILED` with an error
  code, attempt recorded.

## Consequences

- (+) No thread blocking; bounded retries; observable failure classes.
- (-) Delayed retry via re-publish adds a topic hop; documented.

## Alternatives

- Kafka-native delayed retry topics per delay level: more topics; not needed
  at this scale.
- Blocking `Thread.sleep` in the consumer: wastes partitions; rejected.
