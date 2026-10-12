# MailScale Architecture Overview

## Component map

```mermaid
graph TB
  subgraph Client
    SPA[React SPA - Vite + TS]
  end
  subgraph API tier
    API[Spring Boot API - Java 21]
    SEC[Security: JWT + RBAC + tenant filter]
    OBOX[(Transactional Outbox)]
  end
  subgraph Worker tier
    WK[Kotlin Worker - coroutines]
  end
  subgraph Infrastructure
    PG[(PostgreSQL)]
    RD[(Redis)]
    K[(Kafka)]
    SMTP[SMTP / Mailpit]
    LLM[LLM provider / mock]
    PROM[Prometheus]
    GRAF[Grafana]
  end

  SPA --> API
  API --> SEC
  SEC --> PG
  API --> OBOX
  OBOX --> K
  K --> WK
  WK --> PG
  WK --> SMTP
  WK --> K
  API --> RD
  API --> LLM
  API --> PROM
  WK --> PROM
  PROM --> GRAF
```

## Request flow (transactional email)

```mermaid
sequenceDiagram
  participant U as User/API client
  participant A as Spring API
  participant DB as PostgreSQL
  participant K as Kafka
  participant W as Kotlin worker
  participant S as SMTP

  U->>A: POST /api/v1/emails (idempotency-key)
  A->>A: authn/z, rate limit, validate
  A->>A: idempotency check
  A->>DB: tx: insert email_message + outbox event
  A-->>U: 202 Accepted (queued, correlation id)
  A->>K: outbox publisher (polling, claim-based)
  K->>W: consume send command
  W->>DB: suppression + consent checks
  W->>S: deliver via provider adapter
  W->>DB: write delivery_attempt + update message
  W->>K: publish delivery event
  W-->>K: ack
```

## Boundaries and responsibilities

| Component | Owns | Does not own |
| --- | --- | --- |
| React SPA | UI state, routing, forms | business rules; all authorization decisions |
| Spring API | REST contracts, authn/z, validation, outbox writes, analytics reads | direct SMTP delivery |
| Outbox | reliable hand-off of domain events to Kafka | business logic |
| Kafka | durable transport, ordering per key, replay | exactly-once external delivery |
| Kotlin worker | delivery execution, retry/backoff, suppression checks | authorization, API contracts |
| Provider adapters (SMTP/mock) | transport to an email provider | routing decisions |

## Failure handling

1. **API crash after DB commit, before Kafka publish**: the outbox publisher replays
   unpublished events after restart (at-least-once).
2. **Worker crash mid-delivery**: the Kafka offset is committed only after the delivery
   attempt is durably recorded; on restart the message is reprocessed. The provider
   adapter is idempotency-aware (message key = deterministic idempotency key) so a
   repeat delivery is detectable at the provider where supported.
3. **Transient SMTP failure**: retried with exponential backoff + jitter, up to
   `MAIL_MAX_DELIVERY_ATTEMPTS`; then dead-lettered.
4. **Permanent failure (invalid recipient)**: no retry; message marked `FAILED` with an
   error code.
5. **Suppressed/unsubscribed recipient**: delivery is skipped before any SMTP call.

## Delivery guarantees

The pipeline is **at-least-once** from the API to the worker. External SMTP delivery is
best-effort: a provider may accept a message while the worker loses the acknowledgment,
producing a possible duplicate send. We do **not** promise exactly-once email delivery;
we minimize duplicates via deterministic idempotency keys and provider-side dedup where
supported, and we detect duplicates in delivery status updates.

## Concurrency model

| Mechanism | Where | Why |
| --- | --- | --- |
| Tomcat bounded thread pool | API | HTTP serving, bounded connections |
| HikariCP bounded pool (20) | API + worker | DB connection cap |
| Kafka consumer `max.poll.records` + fixed coroutine dispatcher | Worker | bounded parallelism, no unbounded thread creation |
| Scheduled outbox publisher (single-threaded) | API | one publisher loop per instance |
| Redis sliding-window counters | API | rate limiting across instances |

See [kafka.md](kafka.md) for topic design and [../adr/](adr/) for the decisions behind
these choices.
