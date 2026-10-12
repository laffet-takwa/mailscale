import { useQuery } from '@tanstack/react-query'
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { Card, Spinner, Alert, Badge } from '../components/ui'
import { AnalyticsService } from '../api/analytics'
import { errorMessage, formatPercent } from '../lib/utils'

interface StatProps {
  title: string
  value: string | number
  hint?: string
}

function Stat({ title, value, hint }: StatProps) {
  return (
    <Card>
      <p className="text-sm font-medium text-gray-500">{title}</p>
      <p className="mt-2 text-3xl font-semibold text-charcoal-900">{value}</p>
      {hint && <p className="mt-1 text-sm text-gray-500">{hint}</p>}
    </Card>
  )
}

/** Dashboard with real metrics from the analytics API. */
export default function DashboardPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => AnalyticsService.dashboard(),
    refetchInterval: 30_000,
  })

  if (isLoading) {
    return <Spinner label="Loading metrics..." />
  }

  if (error) {
    return (
      <Alert tone="danger" title="Could not load metrics">
        {errorMessage(error)}
      </Alert>
    )
  }

  if (!data) {
    return <Alert tone="warning">No metrics available</Alert>
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-charcoal-900">
          Dashboard
        </h1>
        <Badge tone={data.deliveryRate >= 95 ? 'success' : 'warning'}>
          {formatPercent(data.deliveryRate)} delivery rate
        </Badge>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat title="Total campaigns" value={data.totalCampaigns} />
        <Stat title="Active contacts" value={data.activeContacts} />
        <Stat title="Queued" value={data.queued} />
        <Stat title="Sent today" value={data.sent} />
        <Stat title="Bounced" value={data.bounced} />
        <Stat title="Failed" value={data.failed} />
        <Stat title="Suppressed" value={data.suppressed} />
        <Stat title="Delivery rate" value={formatPercent(data.deliveryRate)} />
      </div>

      <Card>
        <h2 className="mb-4 text-sm font-medium text-gray-700">
          Messages sent (last 7 days)
        </h2>
        {data.sentPerDay.length === 0 ? (
          <p className="py-8 text-center text-sm text-gray-500">
            No data for this period yet.
          </p>
        ) : (
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={data.sentPerDay}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
                <Tooltip />
                <Line
                  type="monotone"
                  dataKey="sent"
                  stroke="#2c4b84"
                  strokeWidth={2}
                  dot={false}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </Card>
    </div>
  )
}
