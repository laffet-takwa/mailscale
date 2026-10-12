# ADR-0004: Idempotency and delivery guarantees

**Status:** Accepted
**Date:** 2026-10-12

## Context

Clients retry HTTP requests; Kafka redelivers after failures; SMTP providers
may accept a message while the worker loses the ack.

## Decision

- **API idempotency**: `POST /api/v1/emails` requires an `Idempotency-Key`.
  The API stores `(organization_id, key)` with a hash of the request and the
  stored response. Replays return the original `202` without re-queueing.
  Keys expire after 24h.
- **Message idempotency**: every `email_messages.id` is a deterministic UUID
  derived from the idempotency key (UUIDv5), so the Kafka message key and the
  DB key are stable across retries.
- **Worker dedupe**: before delivering, the worker checks the message status;
  if already `DELIVERED`/`SENT`, the record is acked without re-delivery.
- **Guarantee level**: at-least-once processing; **not** exactly-once email
  delivery. Duplicates are minimized (deterministic keys, status checks,
  provider dedup where supported) and detected (delivery status queries).

## Consequences

- (+) Safe client retries and Kafka redelivery.
- (-) Honest limitation: an SMTP provider that accepts a message right
  before a worker crash can produce one duplicate send. Documented in the API
  reference and benchmarks.

## Alternatives

- Exactly-once claims: impossible to guarantee end-to-end with external SMTP.
  We refuse to promise it.
