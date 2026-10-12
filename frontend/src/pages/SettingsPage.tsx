import { Card } from '../components/ui'
import { useAuth } from '../lib/useAuth'

interface Row {
  label: string
  value: string
}

/** Organization settings for the current tenant. */
export default function SettingsPage() {
  const { user } = useAuth()

  const orgRows: Row[] = [
    { label: 'Organization', value: user?.organizationSlug ?? '-' },
    { label: 'Your role', value: user?.role ?? '-' },
    { label: 'User ID', value: String(user?.userId ?? '-') },
  ]

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-charcoal-900">Settings</h1>

      <Card>
        <h2 className="mb-4 text-sm font-medium text-gray-700">
          Organization
        </h2>
        <dl className="grid gap-4 sm:grid-cols-2">
          {orgRows.map((row) => (
            <div key={row.label}>
              <dt className="text-sm text-gray-500">{row.label}</dt>
              <dd className="mt-1 font-medium text-charcoal-900">
                {row.value}
              </dd>
            </div>
          ))}
        </dl>
      </Card>

      <Card>
        <h2 className="mb-2 text-sm font-medium text-gray-700">
          Configuration notes
        </h2>
        <ul className="list-disc space-y-1 pl-5 text-sm text-gray-600">
          <li>
            PostgreSQL is the default database; a documented SQL Server
            compatibility profile exists (see docs/adr/0005).
          </li>
          <li>
            Rate limits are enforced with Redis sliding-window counters
            on the transactional email endpoint.
          </li>
          <li>
            The AI assistant drafts content only; it never overrides
            consent, suppression, authorization, or rate limits.
          </li>
          <li>
            Secrets are supplied through environment variables and are
            never committed to the repository.
          </li>
        </ul>
      </Card>
    </div>
  )
}
