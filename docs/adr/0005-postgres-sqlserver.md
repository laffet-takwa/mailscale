# ADR-0005: PostgreSQL default, SQL Server compatibility profile

**Status:** Accepted
**Date:** 2026-10-12

## Context

The platform targets PostgreSQL as the primary database but must demonstrate
SQL Server compatibility knowledge. Both must not need to run simultaneously.

## Decision

- PostgreSQL is the default and the only fully tested database.
- A `sqlserver` Spring profile (`SPRING_SQLSERVER_ENABLED=true`) swaps the
  dialect and Flyway places SQL Server-specific DDL where needed.
- Portability rules for migrations:
  - JSON payloads use `TEXT`/`NVARCHAR(MAX)` (not `JSONB`) with Jackson
    serialization at the entity layer; SQL:2003 types work on all three
    databases (PostgreSQL, SQL Server, H2 for tests).
  - No `SKIP LOCKED`, no vendor-specific functions in application queries;
    pagination uses standard `ORDER BY ... LIMIT/OFFSET` (SQL Server profile
    rewrites to `OFFSET ... FETCH NEXT` via Hibernate dialect).
  - UUID columns use `UUID` on PostgreSQL and `UNIQUEIDENTIFIER` via dialect
    mapping; H2 uses `UUID`.
- Flyway locations: `classpath:db/migration` (portable) plus
  `classpath:db/migration/postgresql` and `.../sqlserver` for
  database-specific statements, selected per profile.

## Consequences

- (+) One schema, one migration set; compatibility is explicit and tested by
  compiling/running the portable tests.
- (-) SQL Server path is verified by migration + smoke tests, not by the full
  integration suite; we state this limitation honestly.

## Alternatives

- Supporting both equally: doubles the test matrix; rejected in favor of one
  primary database plus a documented compatibility profile.
