# MailScale load-testing instructions

## Prerequisites

- The API is running (see docs/DEVELOPMENT.md), pointing at PostgreSQL.
- A JWT token for an organization that owns at least one template.
- k6 installed: https://k6.io/docs/get-started/installation/

## Step 1: obtain a token

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@mailscale.local","password":"demo1234"}' | jq -r .token
```

## Step 2: find a template id

```bash
curl -s http://localhost:8080/api/v1/templates \
  -H "Authorization: Bearer $TOKEN" | jq '.[0].id'
```

## Step 3: run the transactional load test

```bash
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env TOKEN="$TOKEN" \
  --env TEMPLATE_ID=1 \
  load-tests/transactional.js
```

The script has two phases: a 30s burst at 100 messages/s then a 2m soak at
20 messages/s. Results are written to `docs/benchmarks/k6-transactional-*.json`.

## Step 4: observe the pipeline

While the load test runs, watch worker metrics:

```bash
curl -s localhost:8081/metrics | grep mailscale_worker
```

and consumer lag:

```bash
docker compose exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group mailscale-worker --describe
```

## Step 5: campaign dispatch load test

```bash
curl -s -X POST http://localhost:8080/api/v1/campaigns/<id>/dispatch \
  -H "Authorization: Bearer $TOKEN"
```

## Recording results

Store results in `docs/benchmarks/` and update
`docs/benchmarks/README.md` with the machine, dataset size, concurrency
settings, duration, and the observed p50/p95/p99 latency, throughput, and
error rate. Never invent numbers; every benchmark must be reproducible with
the commands above.
