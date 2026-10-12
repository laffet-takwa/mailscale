# k6 load test for the MailScale transactional email API.
#
# Runs against a locally started API (default profile, PostgreSQL).
# The JWT below must be replaced with a token from POST /api/v1/auth/login.
#
# Usage:
#   k6 run --env BASE_URL=http://localhost:8080 \
#         --env TOKEN=<jwt> --env TEMPLATE_ID=1 \
#         load-tests/transactional.js

import http from 'k6/http'
import { check, sleep } from 'k6'
import { Counter, Rate, Trend } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const TOKEN = __ENV.TOKEN || ''
const TEMPLATE_ID = Number(__ENV.TEMPLATE_ID || '1')

const accepted = new Counter('messages_accepted')
const rejected = new Rate('rejected_rate')
const queuedLatency = new Trend('queue_latency_ms')

export const options = {
  // Two scenarios: a short burst to measure peak acceptance, then a
  // steady-state soak that exercises the outbox and Kafka pipeline.
  scenarios: {
    burst: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 20,
      maxVUs: 100,
      startTime: '0s',
      exec: 'sendEmail',
    },
    soak: {
      executor: 'constant-arrival-rate',
      rate: 20,
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 20,
      maxVUs: 60,
      startTime: '40s',
      exec: 'sendEmail',
    },
  },
  thresholds: {
    // The API must accept >99% of requests while under load.
    rejected_rate: ['rate<0.01'],
    // P95 latency to accept a queued message stays under 300ms.
    queue_latency_ms: ['p(95)<300'],
  },
}

export function sendEmail() {
  if (!TOKEN) {
    throw new Error('TOKEN env var is required (JWT from /auth/login)')
  }

  // Unique idempotency key per request: every send is distinct, so
  // retries from the client would still be deduplicated by k6's own
  // idempotent keys.
  const idempotencyKey = `k6-${__VU}-${__ITER}-${Date.now()}`

  const payload = JSON.stringify({
    to: `recipient-${__VU}-${__ITER}@example.com`,
    toName: `Recipient ${__VU}`,
    templateId: TEMPLATE_ID,
    variables: { firstName: 'Load', company: 'LoadTest' },
    idempotencyKey,
  })

  const response = http.post(
    `${BASE_URL}/api/v1/emails`,
    payload,
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${TOKEN}`,
      },
      timeout: '10s',
    }
  )

  queuedLatency.add(response.timings.duration)

  const ok = check(response, {
    'status is 202 Accepted': (r) => r.status === 202,
    'body messageId present': (r) => {
      try {
        return Boolean(JSON.parse(r.body).messageId)
      } catch {
        return false
      }
    },
    'latency under 1s': (r) => r.timings.duration < 1000,
  })

  if (ok) {
    accepted.add(1)
    rejected.add(0)
  } else {
    rejected.add(1)
    console.error(`rejected: status=${response.status} body=${response.body}`)
  }

  sleep(0.1)
}

export function handleSummary(data) {
  const file = `docs/benchmarks/k6-transactional-${Date.now()}.json`
  return {
    file: JSON.stringify(data, null, 2),
    stdout: `
k6 transactional load test
---------------------------
messages accepted : ${data.metrics.messages_accepted?.values?.count ?? 'n/a'}
reject rate       : ${data.metrics.rejected_rate?.values?.rate ?? 'n/a'}
p95 queue latency : ${data.metrics.queue_latency_ms?.values?.['p(95)'] ?? 'n/a'} ms
reported to       : ${file}
`,
  }
}
