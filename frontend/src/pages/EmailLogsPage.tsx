import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  Badge,
  Button,
  Card,
  Input,
  Select,
  Spinner,
} from '../components/ui'
import { DataTable } from '../components/DataTable'
import type { Column } from '../components/DataTable'
import { EmailService } from '../api/campaigns'
import type { EmailMessage, EmailStatus } from '../types'
import { errorMessage, formatDate } from '../lib/utils'

const STATUS_TONES: Record<
  EmailStatus,
  'neutral' | 'success' | 'warning' | 'danger' | 'info'
> = {
  QUEUED: 'neutral',
  PROCESSING: 'info',
  SENT: 'info',
  DELIVERED: 'success',
  BOUNCED: 'danger',
  FAILED: 'danger',
  SUPPRESSED: 'warning',
}

const STATUS_FILTERS = [
  { value: '', label: 'All' },
  { value: 'QUEUED', label: 'Queued' },
  { value: 'PROCESSING', label: 'Processing' },
  { value: 'SENT', label: 'Sent' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'BOUNCED', label: 'Bounced' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'SUPPRESSED', label: 'Suppressed' },
]

/** Transactional email logs with status filtering. */
export default function EmailLogsPage() {
  const [status, setStatus] = useState('')
  const [correlation, setCorrelation] = useState('')
  const [query, setQuery] = useState<{
    status?: EmailStatus
    correlation?: string
  }>({})

  const { data, isLoading, error } = useQuery({
    queryKey: ['email-logs', query],
    queryFn: () =>
      EmailService.list(
        0,
        50,
        query.status as EmailStatus | undefined
      ),
  })

  const statusLookup = useQuery({
    queryKey: ['email-status', query.correlation],
    queryFn: () => EmailService.status(query.correlation as string),
    enabled: Boolean(query.correlation),
  })

  const columns: Array<Column<EmailMessage>> = [
    {
      key: 'toEmail',
      header: 'Recipient',
      render: (message) => (
        <span className="font-medium text-navy-700">
          {message.toEmail}
        </span>
      ),
    },
    {
      key: 'subject',
      header: 'Subject',
      render: (message) => message.subject,
    },
    {
      key: 'status',
      header: 'Status',
      render: (message) => (
        <Badge tone={STATUS_TONES[message.status]}>{message.status}</Badge>
      ),
    },
    {
      key: 'messageType',
      header: 'Type',
      render: (message) => message.messageType,
    },
    {
      key: 'createdAt',
      header: 'Created',
      render: (message) => formatDate(message.createdAt),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (message) => (
        <Button
          variant="secondary"
          onClick={() =>
            setQuery((prev) => ({
              ...prev,
              correlation: message.correlationId,
            }))
          }
        >
          Details
        </Button>
      ),
    },
  ]

  if (isLoading) {
    return <Spinner label="Loading emails..." />
  }

  if (error) {
    return (
      <Alert tone="danger" title="Could not load emails">
        {errorMessage(error)}
      </Alert>
    )
  }

  const messages = data?.content ?? []

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-charcoal-900">
        Email Logs
      </h1>

      <Card>
        <div className="grid gap-4 sm:grid-cols-3">
          <Select
            label="Status"
            options={STATUS_FILTERS}
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          />
          <Input
            label="Correlation ID"
            placeholder="Lookup by correlation"
            value={correlation}
            onChange={(event) => setCorrelation(event.target.value)}
          />
          <div className="flex items-end gap-2">
            <Button
              onClick={() =>
                setQuery({
                  status: status
                    ? (status as EmailStatus)
                    : undefined,
                  correlation: correlation || undefined,
                })
              }
            >
              Apply
            </Button>
            <Button
              variant="secondary"
              onClick={() => {
                setStatus('')
                setCorrelation('')
                setQuery({})
              }}
            >
              Reset
            </Button>
          </div>
        </div>
      </Card>

      {statusLookup.data && (
        <Card>
          <h2 className="mb-2 text-sm font-medium text-gray-700">
            Message {statusLookup.data.correlationId}
          </h2>
          <div className="flex items-center gap-3">
            <Badge tone={STATUS_TONES[statusLookup.data.status]}>
              {statusLookup.data.status}
            </Badge>
            <span className="text-sm text-gray-600">
              {statusLookup.data.toEmail}
            </span>
            <span className="text-sm text-gray-600">
              Provider ID:{' '}
              {statusLookup.data.providerMessageId ?? 'n/a'}
            </span>
          </div>
        </Card>
      )}

      {statusLookup.error && (
        <Alert tone="danger" title="Lookup failed">
          {errorMessage(statusLookup.error)}
        </Alert>
      )}

      <Card>
        <DataTable
          columns={columns}
          rows={messages}
          keyFn={(message) => message.id}
          emptyMessage="No email messages found."
        />
      </Card>
    </div>
  )
}
