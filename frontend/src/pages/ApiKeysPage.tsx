import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Badge, Button, Card, Input, Spinner } from '../components/ui'
import { DataTable } from '../components/DataTable'
import type { Column } from '../components/DataTable'
import { ApiKeyService } from '../api/analytics'
import type { ApiKey } from '../types'
import { errorMessage, formatDate } from '../lib/utils'

/** API key management with one-time secret display. */
export default function ApiKeysPage() {
  const [name, setName] = useState('')
  const [createError, setCreateError] = useState<string | null>(null)
  const [newSecret, setNewSecret] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data, isLoading, error } = useQuery({
    queryKey: ['api-keys'],
    queryFn: () => ApiKeyService.list(),
  })

  const create = useMutation({
    mutationFn: (keyName: string) => ApiKeyService.create(keyName),
    onSuccess: (result) => {
      setNewSecret(result.secret)
      setName('')
      setCreateError(null)
      void queryClient.invalidateQueries({ queryKey: ['api-keys'] })
    },
    onError: (err: unknown) => {
      setCreateError(errorMessage(err))
    },
  })

  const columns: Array<Column<ApiKey>> = [
    {
      key: 'name',
      header: 'Name',
      render: (key) => (
        <span className="font-medium text-navy-700">{key.name}</span>
      ),
    },
    {
      key: 'prefix',
      header: 'Prefix',
      render: (key) => <code className="text-xs">{key.keyPrefix}...</code>,
    },
    {
      key: 'scopes',
      header: 'Scopes',
      render: (key) => key.scopes,
    },
    {
      key: 'lastUsedAt',
      header: 'Last used',
      render: (key) => formatDate(key.lastUsedAt),
    },
    {
      key: 'status',
      header: 'Status',
      render: (key) => (
        <Badge tone={key.revokedAt || key.expiresAt ? 'danger' : 'success'}>
          {key.revokedAt
            ? 'REVOKED'
            : key.expiresAt && new Date(key.expiresAt) < new Date()
              ? 'EXPIRED'
              : 'ACTIVE'}
        </Badge>
      ),
    },
  ]

  if (isLoading) {
    return <Spinner label="Loading API keys..." />
  }

  if (error) {
    return (
      <Alert tone="danger" title="Could not load API keys">
        {errorMessage(error)}
      </Alert>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-charcoal-900">API Keys</h1>
        <p className="mt-1 text-sm text-gray-600">
          API keys authenticate server-to-server requests with the{' '}
          <code className="text-xs">X-API-Key</code> header. Only a SHA-256
          hash is stored; the secret is shown once.
        </p>
      </div>

      {newSecret && (
        <Alert tone="success" title="API key created">
          <p className="mb-2">
            Copy this secret now - it will not be shown again:
          </p>
          <code className="block break-all rounded bg-white p-3 font-mono text-sm">
            {newSecret}
          </code>
          <Button
            variant="secondary"
            className="mt-2"
            onClick={() => setNewSecret(null)}
          >
            I have copied it
          </Button>
        </Alert>
      )}

      <Card>
        <form
          className="flex flex-wrap items-end gap-4"
          onSubmit={(event) => {
            event.preventDefault()
            create.mutate(name)
          }}
        >
          <div className="w-72">
            <Input
              label="Key name"
              placeholder="production-sender"
              value={name}
              onChange={(event) => setName(event.target.value)}
              required
            />
          </div>
          <Button type="submit" isLoading={create.isPending}>
            Create key
          </Button>
        </form>
        {createError && (
          <div className="mt-4">
            <Alert tone="danger" title="Could not create key">
              {createError}
            </Alert>
          </div>
        )}
      </Card>

      <Card>
        <DataTable
          columns={columns}
          rows={data ?? []}
          keyFn={(key) => key.id}
          emptyMessage="No API keys yet. Create one to send email from a service."
        />
      </Card>
    </div>
  )
}
