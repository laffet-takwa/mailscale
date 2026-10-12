import { useQuery } from '@tanstack/react-query'
import { Alert, Badge, Card, Spinner } from '../components/ui'
import { MonitoringService } from '../api/analytics'
import { errorMessage } from '../lib/utils'

const HEALTH_TONES: Record<string, 'success' | 'warning' | 'danger'> = {
  UP: 'success',
  DOWN: 'danger',
  OUT_OF_SERVICE: 'danger',
  UNKNOWN: 'warning',
}

/** System health page driven by Spring Actuator health groups. */
export default function MonitoringPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['system-status'],
    queryFn: () => MonitoringService.status(),
    refetchInterval: 15_000,
  })

  if (isLoading) {
    return <Spinner label="Checking system status..." />
  }

  if (error) {
    return (
      <Alert tone="danger" title="Could not check status">
        {errorMessage(error)}
      </Alert>
    )
  }

  if (!data) {
    return <Alert tone="warning">No status available</Alert>
  }

  const overall = data.overall ?? 'UNKNOWN'
  const components = data.components ?? {}

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-semibold text-charcoal-900">
          System Health
        </h1>
        <Badge tone={HEALTH_TONES[overall] ?? 'warning'}>{overall}</Badge>
      </div>

      <Card>
        <h2 className="mb-4 text-sm font-medium text-gray-700">Components</h2>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {Object.entries(components).map(([name, status]) => (
            <div
              key={name}
              className="flex items-center justify-between rounded-md border border-gray-200 px-4 py-3"
            >
              <span className="text-sm font-medium text-charcoal-900">
                {name}
              </span>
              <Badge tone={HEALTH_TONES[status] ?? 'warning'}>
                {status}
              </Badge>
            </div>
          ))}
        </div>
      </Card>

      <Card>
        <h2 className="mb-2 text-sm font-medium text-gray-700">
          Monitoring endpoints
        </h2>
        <ul className="space-y-1 text-sm text-gray-600">
          <li>
            <code className="text-xs">/actuator/health</code> - liveness and
            readiness
          </li>
          <li>
            <code className="text-xs">/actuator/prometheus</code> - metrics
            scrape endpoint for Prometheus
          </li>
          <li>
            <code className="text-xs">:8081/metrics</code> - Kotlin worker
            metrics (consumer lag, delivery counts)
          </li>
          <li>
            <code className="text-xs">:9090</code> - Prometheus UI,{' '}
            <code className="text-xs">:3000</code> - Grafana
          </li>
        </ul>
      </Card>
    </div>
  )
}
