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
import { CampaignService } from '../api/campaigns'
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

  const campaigns = useMemo(() => {
    const source = data ?? []
    const needle = search.trim().toLowerCase()
    if (!needle) return source
    return source.filter((campaign) =>
      campaign.name.toLowerCase().includes(needle)
    )
  }, [data, search])

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
        <Link to="/campaigns/new">
          <Button>New campaign</Button>
        </Link>
      </div>

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

