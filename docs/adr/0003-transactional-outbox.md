# ADR-0003: Transactional outbox for reliable publishing

**Status:** Accepted
**Date:** 2026-10-12

## Context

Writing the `email_messages` row and publishing to Kafka in separate steps can
lose messages when the process crashes between commit and publish (dual-write
problem).

## Decision

Use a **transactional outbox**:

1. API transaction inserts the message and an `outbox_events` row atomically.
2. A single-threaded scheduled publisher polls unpublished events, claims them
   with a conditional `UPDATE ... WHERE published_at IS NULL`, publishes to
   Kafka, then marks them published.
3. On publish failure the row stays unclaimed and is retried; the claim
   prevents two publisher instances from double-sending.

## Consequences

- (+) No lost messages across crashes; no distributed transaction (no XA).
- (+) Portable: the claim query avoids vendor-specific `SKIP LOCKED`, so the
  same code runs on PostgreSQL, H2 (tests), and SQL Server (compat profile).
- (-) Small publish latency (poll interval, default 1s) — acceptable for
  email.
- (-) At-least-once publishing is possible on crash-after-publish-before-mark;
  worker idempotency handles it.

## Alternatives

- Debezium CDC: operational complexity, not justified here.
- Spring Kafka transactions: couples DB and Kafka transaction managers and is
  not portable to the standalone Kotlin worker.
