# Documentation index

| Document | Contents |
| --- | --- |
| [../README.md](../README.md) | Project overview, quick start, feature list |
| [architecture/overview.md](architecture/overview.md) | Components, boundaries, data flow, failure handling |
| [architecture/kafka.md](architecture/kafka.md) | Topic design, partitioning, consumer groups, lag |
| [adr/](adr/) | 8 architecture decision records |
| [database/schema.md](database/schema.md) | Tables, indexes, transaction boundaries, isolation |
| [api/openapi.md](api/openapi.md) | Endpoint reference and conventions |
| [testing/README.md](testing/README.md) | Test pyramid and commands |
| [benchmarks/README.md](benchmarks/README.md) | Load-test procedure and result template |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Local setup, env vars, troubleshooting |

## Reproducing the test results

Backend:

```bash
./mvnw -f backend-java/pom.xml test
```

Worker:

```bash
./gradlew -p worker-kotlin test
```

Frontend:

```bash
npm --prefix frontend install
npm --prefix frontend test
```

## Known limitations

- **Not a billion-message system.** The repository deliberately avoids claims of
  global scale. Kafka partitions, worker concurrency, and Postgres sizes here
  are sized for a single-node local demonstration.
- **SQL Server compatibility is documented and profile-based, not part of the
  full integration matrix.** The `sqlserver` profile swaps dialect and
  migration locations; real migration execution against SQL Server is not part
  of the default CI runs.
- **OpenTelemetry tracing is correlation-id-only.** Distributed tracing would
  require attaching the OTel agent; this is listed as a future improvement.
- **AI content review is manual.** The assistant proposes content and variants;
  a human schedules or dispatches every campaign. No autonomous sending exists.
- **No real email is sent by default.** `MAIL_PROVID` defaults to `mock`.
  Enabling `smtp` against a real provider is an explicit, opt-in action.

## Future improvements

1. Batch inserts for campaign dispatch (`JdbcTemplate` `addBatch`) to remove
   per-row round trips.
2. Recorded baseline benchmarks committed under `docs/benchmarks/`.
3. A dedicated SQL Server CI job running the portable migrations against
   `mcr.microsoft.com/mssql/server`.
4. OTel Java agent wiring for end-to-end traces.
5. Bounce webhook handling so provider bounces update suppression state
   asynchronously.
6. Template linting (broken `{{variables}}`, missing text alternative).
7. Read replica routing for dashboard queries.
