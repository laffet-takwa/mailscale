import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Badge,
  Button,
  Card,
  Input,
  Spinner,
} from '../components/ui'
import { DataTable } from '../components/DataTable'
import type { Column } from '../components/DataTable'
import { CampaignService, TemplateService } from '../api/campaigns'
import { SegmentService } from '../api/contacts'
import type { Campaign, CampaignStatus } from '../types'
import { errorMessage, formatDate } from '../lib/utils'

const STATUS_TONES: Record<CampaignStatus, 'neutral' | 'success' | 'info' | 'warning' | 'danger'> = {
  DRAFT: 'neutral',
  SCHEDULED: 'info',
  PROCESSING: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'danger',
  FAILED: 'danger',
}

/** Campaign list with creation wizard and actions. */
export default function CampaignsPage() {
  const [showCreate, setShowCreate] = useState(false)
  const [search, setSearch] = useState('')
  const queryClient = useQueryClient()

  const { data, isLoading, error } = useQuery({
    queryKey: ['campaigns'],
    queryFn: () => CampaignService.list(),
  })

  const dispatch = useMutation({
    mutationFn: (id: number) => CampaignService.dispatch(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['campaigns'] })
    },
  })

  const cancel = useMutation({
    mutationFn: (id: number) => CampaignService.cancel(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['campaigns'] })
    },
  })

  const allCampaigns = data ?? []
  const campaigns = useMemo(() => {
    const needle = search.trim().toLowerCase()
    if (!needle) return allCampaigns
    return allCampaigns.filter((campaign) =>
      campaign.name.toLowerCase().includes(needle)
    )
  }, [allCampaigns, search])

  const columns: Array<Column<Campaign>> = [
    {
      key: 'name',
      header: 'Name',
      render: (campaign) => (
        <Link
          to={`/campaigns/${campaign.id}`}
          className="font-medium text-navy-700 hover:text-navy-800"
        >
          {campaign.name}
        </Link>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (campaign) => (
        <Badge tone={STATUS_TONES[campaign.status]}>{campaign.status}</Badge>
      ),
    },
    {
      key: 'recipientsCount',
      header: 'Recipients',
      render: (campaign) => campaign.recipientsCount,
    },
    {
      key: 'scheduledAt',
      header: 'Scheduled',
      render: (campaign) => formatDate(campaign.scheduledAt),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (campaign) => (
        <div className="flex gap-2">
          {(campaign.status === 'DRAFT' || campaign.status === 'SCHEDULED') && (
            <Button
              onClick={() => dispatch.mutate(campaign.id)}
              disabled={dispatch.isPending}
            >
              Dispatch
            </Button>
          )}
          {(campaign.status === 'SCHEDULED' ||
            campaign.status === 'PROCESSING') && (
            <Button
              variant="danger"
              onClick={() => cancel.mutate(campaign.id)}
              disabled={cancel.isPending}
            >
              Cancel
            </Button>
          )}
        </div>
      ),
    },
  ]

  if (isLoading) {
    return <Spinner label="Loading campaigns..." />
  }

  if (error) {
    return (
      <Alert tone="danger" title="Could not load campaigns">
        {errorMessage(error)}
      </Alert>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-charcoal-900">Campaigns</h1>
        <Button onClick={() => setShowCreate((value) => !value)}>
          {showCreate ? 'Close' : 'New campaign'}
        </Button>
      </div>

      {showCreate && (
        <Card>
          <CampaignWizard
            onCreated={() => {
              setShowCreate(false)
              void queryClient.invalidateQueries({ queryKey: ['campaigns'] })
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
          rows={campaigns}
          keyFn={(campaign) => campaign.id}
          emptyMessage="No campaigns yet. Create a campaign to send marketing email."
        />
      </Card>
    </div>
  )
}

function CampaignWizard({ onCreated }: { onCreated: () => void }) {
  const [name, setName] = useState('')
  const [segmentId, setSegmentId] = useState('')
  const [templateId, setTemplateId] = useState('')
  const [scheduledAt, setScheduledAt] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [validation, setValidation] = useState<string[] | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  const segments = useQuery({
    queryKey: ['segments'],
    queryFn: () => SegmentService.list(),
  })
  const templates = useQuery({
    queryKey: ['templates'],
    queryFn: () => TemplateService.list(),
  })

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    setValidation(null)
    if (!segmentId || !templateId) {
      setError('Select a segment and a template')
      return
    }
    setIsSaving(true)
    try {
      const campaign = await CampaignService.create({
        name,
        segmentId: Number(segmentId),
        templateId: Number(templateId),
      })
      const result = await CampaignService.validate(campaign.id)
      if (!result.valid) {
        setValidation(result.problems)
        return
      }
      if (scheduledAt) {
        await CampaignService.schedule(campaign.id, new Date(scheduledAt).toISOString())
      }
      onCreated()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4">
      {error && (
        <Alert tone="danger" title="Could not save campaign">
          {error}
        </Alert>
      )}
      {validation && validation.length > 0 && (
        <Alert tone="warning" title="Campaign validation failed">
          <ul className="list-disc pl-4">
            {validation.map((problem) => (
              <li key={problem}>{problem}</li>
            ))}
          </ul>
        </Alert>
      )}
      <Input
        label="Campaign name"
        value={name}
        onChange={(event) => setName(event.target.value)}
        required
      />
      <div className="space-y-1">
        <label
          htmlFor="segment"
          className="block text-sm font-medium text-charcoal-900"
        >
          Recipient segment
        </label>
        <select
          id="segment"
          className="block w-full rounded-md border-gray-300 shadow-sm focus:border-navy-500 focus:ring-navy-500 sm:text-sm"
          value={segmentId}
          onChange={(event) => setSegmentId(event.target.value)}
        >
          <option value="">Select a segment...</option>
          {(segments.data ?? []).map((segment) => (
            <option key={segment.id} value={segment.id}>
              {segment.name}
            </option>
          ))}
        </select>
      </div>
      <div className="space-y-1">
        <label
          htmlFor="template"
          className="block text-sm font-medium text-charcoal-900"
        >
          Template
        </label>
        <select
          id="template"
          className="block w-full rounded-md border-gray-300 shadow-sm focus:border-navy-500 focus:ring-navy-500 sm:text-sm"
          value={templateId}
          onChange={(event) => setTemplateId(event.target.value)}
        >
          <option value="">Select a template...</option>
          {(templates.data ?? []).map((template) => (
            <option key={template.id} value={template.id}>
              {template.name}
            </option>
          ))}
        </select>
      </div>
      <Input
        label="Schedule (optional)"
        type="datetime-local"
        value={scheduledAt}
        onChange={(event) => setScheduledAt(event.target.value)}
      />
      <Button type="submit" isLoading={isSaving}>
        Create campaign
      </Button>
    </form>
  )
}
