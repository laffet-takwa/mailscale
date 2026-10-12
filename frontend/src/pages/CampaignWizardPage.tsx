import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Card, Input, Select, Spinner } from '../components/ui'
import { CampaignService } from '../api/campaigns'
import { SegmentService } from '../api/contacts'
import { TemplateService } from '../api/campaigns'
import type { ValidationResult } from '../types'
import { errorMessage } from '../lib/utils'

const TONE_HINT =
  'Create a draft, validate the audience, then schedule or dispatch. ' +
  'Campaigns always run the same consent and suppression checks.'

/**
 * Campaign creation wizard: pick a segment and template, validate
 * the audience, then schedule or leave as a draft.
 */
export default function CampaignWizardPage() {
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [segmentId, setSegmentId] = useState('')
  const [templateId, setTemplateId] = useState('')
  const [scheduledAt, setScheduledAt] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [validation, setValidation] = useState<ValidationResult | null>(null)
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
    if (name.trim().length < 2) {
      setError('Campaign name must be at least 2 characters')
      return
    }

    setIsSaving(true)
    try {
      const campaign = await CampaignService.create({
        name: name.trim(),
        segmentId: Number(segmentId),
        templateId: Number(templateId),
      })

      const result = await CampaignService.validate(campaign.id)

      if (!result.valid) {
        setValidation(result)
        return
      }

      if (scheduledAt) {
        await CampaignService.schedule(
          campaign.id,
          new Date(scheduledAt).toISOString()
        )
      }

      navigate(`/campaigns/${campaign.id}`)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-charcoal-900">
          New campaign
        </h1>
        <p className="mt-1 text-sm text-gray-600">{TONE_HINT}</p>
      </div>

      <Card>
        <form onSubmit={onSubmit} className="space-y-4">
          {error && (
            <Alert tone="danger" title="Could not save campaign">
              {error}
            </Alert>
          )}

          {validation && !validation.valid && (
            <Alert tone="warning" title="Campaign validation failed">
              <ul className="list-disc pl-4">
                {validation.problems.map((problem) => (
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

          {segments.isLoading || templates.isLoading ? (
            <Spinner label="Loading options..." />
          ) : (
            <>
              <Select
                label="Recipient segment"
                options={(segments.data ?? []).map((segment) => ({
                  value: segment.id,
                  label: segment.name,
                }))}
                value={segmentId}
                onChange={(event) => setSegmentId(event.target.value)}
              />
              <Select
                label="Template"
                options={(templates.data ?? []).map((template) => ({
                  value: template.id,
                  label: `${template.name} (v${template.activeVersion})`,
                }))}
                value={templateId}
                onChange={(event) => setTemplateId(event.target.value)}
              />
            </>
          )}

          <Input
            label="Schedule (optional)"
            type="datetime-local"
            value={scheduledAt}
            onChange={(event) => setScheduledAt(event.target.value)}
          />

          {segments.error && (
            <Alert tone="danger">
              {errorMessage(segments.error)}
            </Alert>
          )}
          {templates.error && (
            <Alert tone="danger">
              {errorMessage(templates.error)}
            </Alert>
          )}

          <div className="flex gap-2">
            <Button type="submit" isLoading={isSaving}>
              Create campaign
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate('/campaigns')}
            >
              Cancel
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
