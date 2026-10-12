import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { Alert, Badge, Button, Card, Spinner } from '../components/ui'
import { DataTable } from '../components/DataTable'
import type { Column } from '../components/DataTable'
import { CampaignService } from '../api/campaigns'
import type { CampaignRecipient } from '../types'
import { errorMessage, formatDate } from '../lib/utils'

const STATUS_TONES: Record<
  CampaignRecipient['status'],
  'neutral' | 'success' | 'warning' | 'danger' | 'info'
> = {
  PENDING: 'neutral',
  QUEUED: 'info',
  SENT: 'info',
  DELIVERED: 'success',
  BOUNCED: 'danger',
  FAILED: 'danger',
  SUPPRESSED: 'warning',
  SKIPPED: 'warning',
}

/** Campaign detail with recipient-level delivery status. */
export default function CampaignDetailPage() {
  const { id } = useParams()
  const campaignId = Number(id)

  const campaign = useQuery({
    queryKey: ['campaign', campaignId],
    queryFn: () => CampaignService.get(campaignId),
  })

  const recipients = useQuery({
    queryKey: ['campaign-recipients', campaignId],
    queryFn: () => CampaignService.recipients(campaignId),
  })

  if (campaign.isLoading) {
    return <Spinner label="Loading campaign..." />
  }

  if (campaign.error) {
    return (
      <Alert tone="danger" title="Could not load campaign">
        {errorMessage(campaign.error)}
      </Alert>
    )
  }

  const campaignData = campaign.data
  if (!campaignData) {
    return <Alert tone="warning">Campaign not found</Alert>
  }

  const columns: Array<Column<CampaignRecipient>> = [
    {
      key: 'contactId',
      header: 'Contact ID',
      render: (recipient) => recipient.contactId,
    },
    {
      key: 'status',
      header: 'Status',
      render: (recipient) => (
        <Badge tone={STATUS_TONES[recipient.status]}>
          {recipient.status}
        </Badge>
      ),
    },
    {
      key: 'attempts',
      header: 'Attempts',
      render: (recipient) => recipient.attempts,
    },
    {
      key: 'errorCode',
      header: 'Error',
      render: (recipient) => recipient.errorCode ?? '-',
    },
    {
      key: 'deliveredAt',
      header: 'Delivered',
      render: (recipient) => formatDate(recipient.deliveredAt),
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-charcoal-900">
          {campaignData.name}
        </h1>
        {(campaignData.status === 'SCHEDULED' ||
          campaignData.status === 'PROCESSING') && (
          <Button
            variant="danger"
            onClick={() =>
              CampaignService.cancel(campaignId).then(() =>
                campaign.refetch()
              )
            }
          >
            Cancel campaign
          </Button>
        )}
      </div>

      <Card>
        <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <dt className="text-sm text-gray-500">Status</dt>
            <dd className="mt-1 font-medium text-charcoal-900">
              {campaignData.status}
            </dd>
          </div>
          <div>
            <dt className="text-sm text-gray-500">Recipients</dt>
            <dd className="mt-1 font-medium text-charcoal-900">
              {campaignData.recipientsCount}
            </dd>
          </div>
          <div>
            <dt className="text-sm text-gray-500">Scheduled</dt>
            <dd className="mt-1 font-medium text-charcoal-900">
              {formatDate(campaignData.scheduledAt)}
            </dd>
          </div>
          <div>
            <dt className="text-sm text-gray-500">Completed</dt>
            <dd className="mt-1 font-medium text-charcoal-900">
              {formatDate(campaignData.completedAt)}
            </dd>
          </div>
        </dl>
        {campaignData.failureReason && (
          <p className="mt-4 text-sm text-red-600">
            Failure: {campaignData.failureReason}
          </p>
        )}
      </Card>

      <Card>
        <h2 className="mb-4 text-sm font-medium text-gray-700">Recipients</h2>
        {recipients.isLoading ? (
          <Spinner label="Loading recipients..." />
        ) : recipients.error ? (
          <Alert tone="danger">{errorMessage(recipients.error)}</Alert>
        ) : (
          <DataTable
            columns={columns}
            rows={recipients.data ?? []}
            keyFn={(recipient) => recipient.id}
            emptyMessage="No recipients recorded for this campaign."
          />
        )}
      </Card>
    </div>
  )
}
