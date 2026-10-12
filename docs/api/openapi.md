# API reference

All endpoints are under `/api/v1`. Swagger UI is available at
`/swagger-ui.html` and the OpenAPI document at `/api-docs`.

## Conventions

- Authentication: `Authorization: Bearer <jwt>` or `X-API-Key: <key>`.
- Tenant context is derived from the authenticated principal, never from the
  request body. There is no `organizationId` parameter in any endpoint.
- Pagination: `?page=&size=` (Spring `Pageable`).
- Correlation: send `X-Correlation-ID` to correlate a request end-to-end; the
  API echoes it in the response and logs it.
- Errors use a single schema:

```json
{
  "timestamp": "2026-10-12T10:00:00Z",
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "email: must not be blank",
  "path": "/api/v1/emails",
  "correlationId": "b3f1..."
}
```

## Authentication

### POST /api/v1/auth/register

```json
{
  "email": "owner@example.com",
  "password": "password123",
  "fullName": "Owner Name",
  "organizationName": "Acme",
  "organizationSlug": "acme"
}
```

Response: `{ "token": "...", "userId": 1, "organizationId": 1,
"organizationSlug": "acme", "role": "OWNER" }`

### POST /api/v1/auth/login

```json
{ "email": "admin@mailscale.local", "password": "demo1234" }
```

### GET /api/v1/auth/me

Returns the current user and organization.

### POST /api/v1/auth/api-keys

```json
{ "name": "production-sender", "expiresInDays": 90 }
```

The `secret` is returned **once** and stored only as a SHA-256 hash.

## Contacts

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/contacts?page&size&q&status` | List/filter contacts |
| POST | `/contacts` | Create a contact |
| GET | `/contacts/{id}` | Get a contact |
| PUT | `/contacts/{id}` | Update a contact |
| DELETE | `/contacts/{id}` | Delete a contact |
| PATCH | `/contacts/{id}/unsubscribe` | Unsubscribe + suppress |
| PATCH | `/contacts/{id}/resubscribe` | Resubscribe |
| POST | `/contacts/import` | CSV import (max 5 MB, 10k rows) |

### CSV import

```json
{ "csv": "email,firstName,lastName\nanna@example.com,Anna,Smith" }
```

Response reports processed rows, duplicates, and row-level errors:

```json
{ "processed": 1, "duplicates": [], "errors": [] }
```

## Segments

`GET`/`POST`/`PUT`/`DELETE` `/segments`, plus `GET /segments/{id}/evaluate`
which returns the paginated membership list.

## Templates

`GET`/`POST`/`GET {id}`/`PUT {id}` (new version)/`DELETE {id}` and
`POST /templates/{id}/preview`.

### Create

```json
{
  "name": "Welcome",
  "subject": "Welcome {{firstName}}!",
  "htmlBody": "<p>Hello {{firstName}}</p>",
  "textBody": "Hello {{firstName}}"
}
```

## Campaigns

`GET`/`POST` `/campaigns`, `GET/PATCH /campaigns/{id}`,
`GET /campaigns/{id}/recipients`, `GET /campaigns/{id}/validate`,
`POST /campaigns/{id}/schedule`, `POST /campaigns/{id}/dispatch`,
`POST /campaigns/{id}/cancel`.

### Schedule

```json
{ "scheduledAt": "2026-10-20T09:00:00Z" }
```

### Validate

Returns `{ "valid": true, "problems": [] }`. A campaign may not be scheduled
or dispatched while `problems` is non-empty.

## Transactional email

### POST /api/v1/emails

```json
{
  "to": "recipient@example.com",
  "templateId": 1,
  "variables": { "firstName": "Takwa" },
  "idempotencyKey": "unique-request-key"
}
```

Response: `202 Accepted`

```json
{
  "messageId": "9f1c...",
  "correlationId": "a72b...",
  "status": "QUEUED"
}
```

Behaviour:

- **Validation**: recipient format, template ownership, and suppression /
  consent state are checked. A suppressed or non-consenting recipient is
  rejected with `409` and an explanatory message.
- **Idempotency**: the `idempotencyKey` in the body is required. Replaying the
  same key with an identical request returns the original 202 response; the
  same key with a different request returns `409 CONFLICT`.
- **Rate limiting**: `429 RATE_LIMITED` above
  `mailscale.mail.rate-limit-per-minute` per organization (Redis).
- **Async**: the 202 response means *queued*, not delivered.

### GET /api/v1/emails/{messageId} and GET /api/v1/emails/correlation/{correlationId}

Returns the message with its current status.

## Suppressions

`GET`/`POST`/`DELETE` `/suppressions`.

## Analytics

`GET /analytics/dashboard` returns queued/sent/bounced/failed/suppressed
counts, delivery rate, and a 7-day series.
`GET /analytics/campaigns` returns per-campaign status breakdowns.

## AI assistant

`POST /ai/generate` with a brief, tone, and variant count returns structured
content. The mock provider is used by default so no API key is required.

## Monitoring

`GET /monitoring/status` returns actuator health grouped by component.
`/actuator/health`, `/actuator/prometheus`, `/actuator/metrics` expose the
standard actuator surface.
