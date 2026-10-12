# ADR-0001: Java API and Kotlin worker boundaries

**Status:** Accepted
**Date:** 2026-10-12

## Context

The platform needs an HTTP API tier and a long-running, concurrent email
delivery tier. The team wants to demonstrate both Java and Kotlin meaningfully.

## Decision

- **API**: Java 21 + Spring Boot 3.5 modular monolith (`backend-java`). REST
  contracts, authn/z, validation, transactional writes, outbox, analytics.
- **Worker**: Kotlin 2.1 standalone Gradle app (`worker-kotlin`) with plain
  `kafka-clients`, `kotlinx.coroutines`, JDBC + HikariCP, JavaMail. No Spring.

The worker talks to the *same* database schema (Flyway migrations are shared
by copying the API's migration files into the worker at build time) and to the
*same* Kafka cluster, but shares no code with the API. Communication is
exclusively via Kafka + the database.

## Consequences

- (+) Clear ownership: delivery concurrency, retries, and backoff live in one
  deployable unit that can be scaled independently of the API.
- (+) Kotlin coroutines give structured concurrency for bounded parallel
  delivery with minimal threads.
- (-) Two build systems, two deployment artifacts, schema duplication via
  migration copy (mitigated by build-time copy + migration tests).

## Alternatives

- Single Spring app with an in-process worker: simpler, but couples API
  scaling to delivery scaling and does not demonstrate a separately deployable
  component.
