# Database schema

PostgreSQL is the primary database. A documented SQL Server compatibility
profile exists under the `sqlserver` Spring profile (see
`docs/adr/0005-postgres-sqlserver.md`). All migrations are portable SQL
written to run on PostgreSQL, H2 (tests), and SQL Server.

Migrations live in `backend-java/src/main/resources/db/migration/`.

## Tables

| Table | Purpose |
| --- | --- |
| `organizations` | Tenant record |
| `users` | Global users (email unique across tenants) |
| `organization_members` | Membership + role (`OWNER`, `ADMIN`, `MARKETER`, `VIEWER`) |
| `api_keys` | Hashed API keys for service-to-service auth |
| `contacts` | Recipients (unique per organization) |
| `contact_tags` | Tag taxonomy per organization |
| `contact_tag_members` | Many-to-many join for tags |
| `segments` | JSON audience definitions |
| `templates` | Template identity (active version pointer) |
| `template_versions` | Versioned subject/HTML/text bodies |
| `campaigns` | Campaign lifecycle state |
| `campaign_recipients` | Per-recipient delivery tracking |
| `email_messages` | One row per queued message (transactional or campaign) |
| `delivery_attempts` | One row per provider attempt (retry audit trail) |
| `suppression_entries` | Unsubscribed/bounced/manual suppressions |
| `outbox_events` | Transactional outbox for reliable Kafka publishing |
| `audit_events` | Security-relevant actions |
| `idempotency_records` | Cached API responses keyed by idempotency key |

## Isolation and integrity

- Every tenant-scoped table has `organization_id`.
- Unique constraints enforce business rules across the whole tenant:
  - `uk_contacts_org_email (organization_id, email)`
  - `uk_templates_org_name (organization_id, name)`
  - `uk_campaign_recipients (campaign_id, contact_id)`
  - `uk_suppression_org_email (organization_id, email)`
  - `uk_idempotency_org_key (organization_id, idem_key)`
- Foreign keys are declared for all cross-table references.
- Child rows (`campaign_recipients`, `template_versions`) delete with parent.

## Indexing decisions

| Index | Purpose | Complexity |
| --- | --- | --- |
| `uk_contacts_org_email` | Lookups and dedup by email | O(log n) |
| `idx_contacts_org_updated` | Sorted lists, import pagination | O(log n + k) |
| `idx_email_messages_org_created` | Dashboard and log pagination (newest first) | O(log n + k) |
| `idx_email_messages_correlation` | Status lookup by correlation ID | O(log n) |
| `idx_campaign_recipients_status` | Campaign status aggregation | O(log n + k) |
| `idx_outbox_events_unpublished` | Outbox publisher selection | O(log n + k) |
| `idx_audit_events_org_created` | Audit history | O(log n + k) |

Count queries used by the dashboard (`countByOrganizationIdAndStatusIn...`)
rely on `idx_email_messages_org_created`, so dashboard cost is O(days x log n)
rather than a full table scan.

## Transaction boundaries

| Operation | Boundary |
| --- | --- |
| Queue a transactional email | One transaction: insert message + outbox event + idempotency record |
| Outbox publisher publish | Claim (`UPDATE ... WHERE locked_at IS NULL`), publish, mark published |
| Campaign dispatch | One transaction per batch of 500 recipients (message + outbox + recipient update) |
| Worker delivery | Attempt insert + message status update in one transaction |

## Transaction isolation

The default PostgreSQL isolation level (READ COMMITTED) is used throughout.
Outbox claiming uses a conditional `UPDATE` so two publisher instances never
double-publish the same event. Campaign dispatch is idempotent through the
`uk_campaign_recipients` unique constraint plus a status check.

## JSON payloads

JSON columns are stored as `TEXT` and serialized with Jackson at the entity
layer. This keeps the schema portable across PostgreSQL, SQL Server, and H2
without vendor-specific `JSONB`/`NVARCHAR(MAX)` divergence.

## Retention

- `idempotency_records`: rows expire 24h after creation (`expires_at`).
- `outbox_events`: published rows are deleted after 7 days.
- `audit_events`: retained indefinitely; a documented retention policy is a
  future improvement (see docs/README.md "Limitations").
