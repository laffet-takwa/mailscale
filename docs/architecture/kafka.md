# Kafka Topic Design

Bootstrap servers: `KAFKA_BOOTSTRAP_SERVERS`. Topics are created by the API at startup
via `KafkaAdmin` (auto-create disabled in the broker).

## Topics

| Topic | Produced by | Consumed by | Key | Retention | Purpose |
| --- | --- | --- | --- | --- | --- |
| `mailscale.emails.send` | API (outbox publisher) | Worker | `organizationId` | 7d | Single-transactional-email send commands |
| `mailscale.emails.send.dlq` | Worker (failed) | Operators / replay tooling | `organizationId` | 30d | Dead-lettered messages after max attempts |
| `mailscale.delivery.events` | Worker | API (analytics consumer) | `messageId` | 7d | Delivery status events for dashboards |
| `mailscale.campaigns.dispatch` | API (scheduler) | Worker | `campaignId` | 7d | Campaign batch dispatch commands |
| `mailscale.campaigns.dispatch.dlq` | Worker | Operators | `campaignId` | 30d | Failed campaign batches |

## Partitioning and ordering

- Commands are keyed by organization (email) or message/campaign id so ordering is
  preserved per aggregate.
- Consumer groups: `mailscale-worker` (email commands), `mailscale-api-analytics`
  (delivery events). Scaling consumers within a group scales parallelism.

## Consumer lag

The worker exposes `mailscale_worker_consumer_lag` (via Micrometer Kafka metrics
binding) and the API exposes `kafka_consumer_lag` for the analytics consumer. Alert
thresholds are in `infrastructure/prometheus/alerts.yml`.

## Delivery semantics

- At-least-once processing. Offsets commit only after the delivery attempt is
  persisted (manual `AckMode`/commit after DB write).
- DLQ policy: after `MAIL_MAX_DELIVERY_ATTEMPTS`, the original record is published to
  the DLQ with error headers (`errorCode`, `attempts`, `lastError`) and acked.

## Why Kafka (not synchronous delivery)

See ADR-0002. Summary: decouples API latency from provider latency, enables retry
without blocking callers, buffers bursts, and provides durable replay for recovery.
