import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'

import {
  Alert,
  Button,
  Card,
  Input,
  Select,
  Spinner,
} from '../components/ui'
import { AiService } from '../api/analytics'
import type { AiGenerationRequest, GeneratedContent } from '../types'
import { errorMessage } from '../lib/utils'

const TONE_OPTIONS = [
  { value: 'PROFESSIONAL', label: 'Professional' },
  { value: 'FRIENDLY', label: 'Friendly' },
  { value: 'URGENT', label: 'Urgent' },
  { value: 'MINIMAL', label: 'Minimal' },
]

/**
 * AI assistant page. Generated content is a draft only: campaigns
 * still require explicit validation and dispatch by the user.
 */
export default function AiAssistantPage() {
  const [brief, setBrief] = useState('')
  const [product, setProduct] = useState('')
  const [audience, setAudience] = useState('')
  const [tone, setTone] =
    useState<AiGenerationRequest['tone']>('PROFESSIONAL')
  const [variantCount, setVariantCount] = useState(1)
  const [generated, setGenerated] = useState<GeneratedContent | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)

  const generate = useMutation({
    mutationFn: (data: AiGenerationRequest) => AiService.generate(data),
    onSuccess: (result) => {
      setGenerated(result)
      setSubmitError(null)
    },
    onError: (error: unknown) => {
      setSubmitError(errorMessage(error))
      setGenerated(null)
    },
  })

  const onSubmit = (event: React.FormEvent) => {
    event.preventDefault()
    if (brief.trim().length < 10) {
      setSubmitError('Brief must be at least 10 characters')
      return
    }
    generate.mutate({
      brief,
      product: product || undefined,
      audience: audience || undefined,
      tone,
      variantCount,
      includeCtas: true,
    })
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-charcoal-900">
          AI Assistant
        </h1>
        <p className="mt-1 text-sm text-gray-600">
          Generate email copy drafts. Content requires human review
          before it is used in a campaign.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <form onSubmit={onSubmit} className="space-y-4">
            {submitError && (
              <Alert tone="danger" title="Generation failed">
                {submitError}
              </Alert>
            )}
            <div className="space-y-1">
              <label
                htmlFor="brief"
                className="block text-sm font-medium text-charcoal-900"
              >
                Brief
              </label>
              <textarea
                id="brief"
                rows={4}
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-navy-500 focus:ring-navy-500 sm:text-sm"
                placeholder="Announce our Q1 product launch to existing customers"
                value={brief}
                onChange={(event) => setBrief(event.target.value)}
                required
              />
            </div>
            <Input
              label="Product (optional)"
              value={product}
              onChange={(event) => setProduct(event.target.value)}
            />
            <Input
              label="Audience (optional)"
              value={audience}
              onChange={(event) => setAudience(event.target.value)}
            />
            <div className="grid gap-4 sm:grid-cols-2">
              <Select
                label="Tone"
                options={TONE_OPTIONS}
                value={tone ?? ''}
                onChange={(event) =>
                  setTone(event.target.value as AiGenerationRequest['tone'])
                }
              />
              <Input
                label="Variants"
                type="number"
                min={1}
                max={5}
                value={variantCount}
                onChange={(event) =>
                  setVariantCount(
                    Math.min(5, Math.max(1, Number(event.target.value) || 1))
                  )
                }
              />
            </div>
            <Button type="submit" isLoading={generate.isPending}>
              Generate content
            </Button>
          </form>
        </Card>

        <Card>
          <h2 className="mb-4 text-sm font-medium text-gray-700">
            Generated draft
          </h2>
          {generate.isPending && (
            <Spinner label="Generating with AI provider..." />
          )}
          {generated && (
            <div className="space-y-4">
              <div>
                <p className="text-xs font-medium uppercase text-gray-500">
                  Subject
                </p>
                <p className="mt-1 text-sm font-medium text-charcoal-900">
                  {generated.subject}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase text-gray-500">
                  Text body
                </p>
                <p className="mt-1 whitespace-pre-wrap text-sm text-charcoal-900">
                  {generated.textBody}
                </p>
              </div>
              {generated.callsToAction.length > 0 && (
                <div>
                  <p className="text-xs font-medium uppercase text-gray-500">
                    Calls to action
                  </p>
                  <ul className="mt-1 list-disc pl-5 text-sm text-charcoal-900">
                    {generated.callsToAction.map((cta) => (
                      <li key={cta}>{cta}</li>
                    ))}
                  </ul>
                </div>
              )}
              <div>
                <p className="text-xs font-medium uppercase text-gray-500">
                  Variants
                </p>
                <ul className="mt-1 space-y-1 text-sm text-charcoal-900">
                  {generated.variants.map((variant) => (
                    <li key={variant.subject}>
                      <span className="font-medium">
                        {variant.subject}
                      </span>{' '}
                      - {variant.previewText}
                    </li>
                  ))}
                </ul>
              </div>
              <p className="text-xs text-gray-500">
                Provider: {generated.provider} Â· latency{' '}
                {generated.latencyMs}ms
              </p>
            </div>
          )}
          {!generated && !generate.isPending && (
            <p className="py-12 text-center text-sm text-gray-500">
              No content generated yet.
            </p>
          )}
        </Card>
      </div>
    </div>
  )
}
