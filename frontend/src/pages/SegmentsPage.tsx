import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Card, Input, Spinner } from '../components/ui'
import { DataTable } from '../components/DataTable'
import type { Column } from '../components/DataTable'
import { SegmentService } from '../api/contacts'
import type { Segment } from '../types'
import { errorMessage } from '../lib/utils'

/** Segments list with evaluation member counts. */
export default function SegmentsPage() {
  const [search, setSearch] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const queryClient = useQueryClient()

  const { data, isLoading, error } = useQuery({
    queryKey: ['segments'],
    queryFn: () => SegmentService.list(),
  })

  const remove = useMutation({
    mutationFn: (id: number) => SegmentService.remove(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['segments'] })
    },
  })

  const allSegments = data ?? []
  const segments = useMemo(() => {
    const needle = search.trim().toLowerCase()
    if (!needle) return allSegments
    return allSegments.filter((segment) =>
      segment.name.toLowerCase().includes(needle)
    )
  }, [allSegments, search])

  const columns: Array<Column<Segment>> = [
    {
      key: 'name',
      header: 'Name',
      render: (segment) => (
        <span className="font-medium text-navy-700">{segment.name}</span>
      ),
    },
    {
      key: 'definition',
      header: 'Definition',
      render: (segment) => (
        <code className="text-xs text-gray-600">
          {segment.definition.length > 60
            ? `${segment.definition.slice(0, 60)}...`
            : segment.definition}
        </code>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (segment) => (
        <Button
          variant="danger"
          onClick={() => remove.mutate(segment.id)}
        >
          Delete
        </Button>
      ),
    },
  ]

  if (isLoading) {
    return <Spinner label="Loading segments..." />
  }

  if (error) {
    return (
      <Alert tone="danger" title="Could not load segments">
        {errorMessage(error)}
      </Alert>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-charcoal-900">Segments</h1>
        <Button onClick={() => setShowCreate((value) => !value)}>
          {showCreate ? 'Close' : 'New segment'}
        </Button>
      </div>

      {showCreate && (
        <Card>
          <CreateSegmentForm
            onCreated={() => {
              setShowCreate(false)
              void queryClient.invalidateQueries({ queryKey: ['segments'] })
            }}
          />
        </Card>
      )}

      <Card>
        <div className="mb-4 max-w-sm">
          <Input
            label="Filter"
            placeholder="Filter by name"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
        <DataTable
          columns={columns}
          rows={segments}
          keyFn={(segment) => segment.id}
          emptyMessage="No segments yet. Create one to target campaigns."
        />
      </Card>
    </div>
  )
}

function CreateSegmentForm({ onCreated }: { onCreated: () => void }) {
  const [name, setName] = useState('')
  const [definition, setDefinition] = useState(
    '{"match":"ALL","rules":[{"field":"status","operator":"EQUALS","value":"ACTIVE"}]}'
  )
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  return (
    <form
      className="space-y-4"
      onSubmit={async (event) => {
        event.preventDefault()
        setError(null)
        setIsSaving(true)
        try {
          await SegmentService.create({ name, definition })
          onCreated()
        } catch (err) {
          setError(errorMessage(err))
        } finally {
          setIsSaving(false)
        }
      }}
    >
      {error && (
        <Alert tone="danger" title="Could not save segment">
          {error}
        </Alert>
      )}
      <Input
        label="Name"
        name="name"
        value={name}
        onChange={(event) => setName(event.target.value)}
        required
      />
      <div className="space-y-1">
        <label
          htmlFor="definition"
          className="block text-sm font-medium text-charcoal-900"
        >
          Definition (JSON)
        </label>
        <textarea
          id="definition"
          rows={4}
          className="block w-full rounded-md border-gray-300 font-mono text-xs shadow-sm focus:border-navy-500 focus:ring-navy-500"
          value={definition}
          onChange={(event) => setDefinition(event.target.value)}
          required
        />
      </div>
      <Button type="submit" isLoading={isSaving}>
        Create segment
      </Button>
    </form>
  )
}
