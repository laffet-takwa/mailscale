import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
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
import { SuppressionService } from '../api/analytics'
import type { SuppressionEntry, SuppressionReason } from '../types'
import { errorMessage, formatDate } from '../lib/utils'

const REASON_TONES: Record<
  SuppressionReason,
  'warning' | 'danger' | 'neutral'
> = {
  UNSUBSCRIBE: 'warning',
  BOUNCE: 'danger',
  MANUAL: 'neutral',
  COMPLAINT: 'danger',
}

const REASON_OPTIONS = [
  { value: 'MANUAL', label: 'Manual' },
  { value: 'UNSUBSCRIBE', label: 'Unsubscribe' },
  { value: 'BOUNCE', label: 'Bounce' },
  { value: 'COMPLAINT', label: 'Complaint' },
]

/** Suppression list management. */
export default function SuppressionPage() {
  const [email, setEmail] = useState('')
  const [reason, setReason] = useState<SuppressionReason>('MANUAL')
  const [formError, setFormError] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data, isLoading, error } = useQuery({
    queryKey: ['suppressions'],
    queryFn: () => SuppressionService.list(),
  })

  const remove = useMutation({
    mutationFn: (id: number) => SuppressionService.remove(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['suppressions'] })
    },
  })

  const columns: Array<Column<SuppressionEntry>> = [
    {
      key: 'email',
      header: 'Email',
      render: (entry) => (
        <span className="font-medium text-navy-700">{entry.email}</span>
      ),
    },
    {
      key: 'reason',
      header: 'Reason',
      render: (entry) => (
        <Badge tone={REASON_TONES[entry.reason]}>{entry.reason}</Badge>
      ),
    },
    {
      key: 'createdAt',
      header: 'Added',
      render: (entry) => formatDate(entry.createdAt),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (entry) => (
        <Button
          variant="danger"
          onClick={() => remove.mutate(entry.id)}
          disabled={remove.isPending}
        >
          Remove
        </Button>
      ),
    },
  ]

  if (isLoading) {
    return <Spinner label="Loading suppression list..." />
  }

  if (error) {
    return (
      <Alert tone="danger" title="Could not load suppression list">
        {errorMessage(error)}
      </Alert>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-charcoal-900">
          Suppression List
        </h1>
        <p className="mt-1 text-sm text-gray-600">
          Suppressed recipients never receive campaign or transactional email.
          The worker checks this list before any SMTP call.
        </p>
      </div>

      <Card>
        <form
          className="flex flex-wrap items-end gap-4"
          onSubmit={async (event) => {
            event.preventDefault()
            setFormError(null)
            try {
              await SuppressionService.add({ email, reason })
              setEmail('')
              void queryClient.invalidateQueries({
                queryKey: ['suppressions'],
              })
            } catch (err) {
              setFormError(errorMessage(err))
            }
          }}
        >
          <div className="w-64">
            <Input
              label="Email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>
          <div className="w-48">
            <Select
              label="Reason"
              options={REASON_OPTIONS}
              value={reason}
              onChange={(event) =>
                setReason(event.target.value as SuppressionReason)
              }
            />
          </div>
          <Button type="submit">Add</Button>
        </form>
        {formError && (
          <div className="mt-4">
            <Alert tone="danger" title="Could not add">
              {formError}
            </Alert>
          </div>
        )}
      </Card>

      <Card>
        <DataTable
          columns={columns}
          rows={data ?? []}
          keyFn={(entry) => entry.id}
          emptyMessage="No suppressed recipients."
        />
      </Card>
    </div>
  )
}
