# ADR-0002: Kafka for asynchronous delivery (not synchronous SMTP)

**Status:** Accepted
**Date:** 2026-10-12

## Context

Transactional emails must be sent without making the API call wait on SMTP
round-trips, and marketing campaigns must absorb bursts of thousands of
recipients.

## Decision

The API persists the message and an outbox event in one transaction; an
outbox publisher forwards events to Kafka; the Kotlin worker consumes and
delivers. The API returns `202 Accepted` with a correlation id and a status
endpoint.

## Consequences

- (+) API p99 latency is independent of provider latency and outages.
- (+) Kafka buffers bursts; the worker drains at a controlled rate.
- (+) Durable replay after worker/API crashes.
- (-) Eventual consistency of delivery status (documented in the API).
- (-) Operational cost of running Kafka (acceptable for this project via
  docker-compose; KRaft mode, no ZooKeeper).

## Alternatives

- Synchronous SMTP from the API: simplest, but provider failures become API
  failures and bursts tie up request threads. Rejected for a messaging
  platform whose core value is reliable delivery.
