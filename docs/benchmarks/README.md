# Benchmarks

All numbers in this repository must be measured with the commands below and
recorded with the machine, dataset, concurrency settings, duration, and
limitations. **No invented results.**

## Environment template

Record these for every benchmark:

- CPU: model and core count
- RAM
- OS and version
- JVM version and heap settings (`-Xmx`)
- PostgreSQL version and connection pool size
- Kafka broker count and partition count
- Worker `WORKER_CONCURRENCY` and `WORKER_MAX_POLL_RECORDS`
- Dataset size (number of contacts / messages)

## 1. Transactional email API (k6)

```bash
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env TOKEN="$TOKEN" \
  --env TEMPLATE_ID=1 \
  load-tests/transactional.js
```

The script runs a 30s burst at 100 messages/s, then a 2m soak at 20
messages/s. k6 writes a JSON summary into `docs/benchmarks/`.

Metrics to record:

| Metric | Source |
| --- | --- |
| Messages accepted per second | `messages_accepted` |
| Requests rejected | `rejected_rate` |
| p50 / p95 queue latency | `queue_latency_ms` |
| API p95 / p99 | `http_client_req_duration` |
| Error rate | `http_req_failed` |

## 2. Worker pipeline throughput

While the load test runs, scrape worker metrics:

```bash
curl -s localhost:8081/metrics | grep -E 'consumed|processed|failed|retries|lag'
```

Record:

| Metric | Micrometer name |
| --- | --- |
| Consumed per second | `mailscale_worker_messages_consumed_total` |
| Processed per second | `mailscale_worker_messages_processed_total` |
| Failures by class | `mailscale_worker_failures_by_class_total` |
| Delivery latency p50/p95/p99 | `mailscale_worker_delivery_duration_seconds` |
| Consumer lag | `mailscale_worker_consumer_lag` |

## 3. Kafka consumer lag

```bash
docker compose exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group mailscale-worker --describe
```

## 4. Campaign dispatch (batch)

Seed N contacts (CSV import is O(n) per row with a per-row insert; batch
insert is a documented future improvement), then dispatch a campaign and
record the end-to-end wall time.

## Recorded results

No benchmark numbers have been recorded in this repository yet. Running the
commands above against a documented machine configuration and committing the
k6 JSON output to this directory is the intended workflow. The roadmap in
`docs/README.md` lists recording a baseline benchmark as an open item.

## Known limitations

- The API writes one message + one outbox row per transactional email within a
  single transaction; throughput is bounded by that transaction's cost.
- Campaign dispatch batches 500 recipients per transaction; larger audiences
  take proportionally longer but memory stays bounded.
- The outbox publisher polls once per second by default, adding up to ~1s of
  queueing latency before Kafka.
- Worker delivery concurrency is bounded by `WORKER_CONCURRENCY` threads and
  the provider rate limiter; SMTP round-trips dominate at high throughput.
