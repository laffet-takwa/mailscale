import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import {
  Alert,
  Badge,
  Button,
  Card,
  Input,
  Spinner,
  Textarea,
} from '../components/ui'
import { TemplateService } from '../api/campaigns'
import type { TemplateFormData } from '../types'
import { errorMessage } from '../lib/utils'

interface FormErrors {
  name?: string
  subject?: string
  htmlBody?: string
  textBody?: string
}

function validate(values: TemplateFormData): FormErrors {
  const errors: FormErrors = {}
  if (values.name.trim().length < 2) {
    errors.name = 'Name must be at least 2 characters'
  }
  if (values.subject.trim().length < 2) {
    errors.subject = 'Subject is required'
  }
  if (values.htmlBody.trim().length === 0) {
    errors.htmlBody = 'HTML body is required'
  }
  if (values.textBody.trim().length === 0) {
    errors.textBody = 'Text body is required'
  }
  return errors
}

/**
 * Template editor with validation, live preview, and versioned
 * saving (creates a new version when editing an existing template).
 */
export default function TemplateEditorPage() {
  const { id } = useParams()
  const isNew = id === undefined || id === 'new'
  const templateId = isNew ? null : Number(id)

  const { data, isLoading, error } = useQuery({
    queryKey: ['template', templateId],
    queryFn: () => TemplateService.get(templateId as number),
    enabled: templateId !== null,
  })

  const [values, setValues] = useState<TemplateFormData>({
    name: '',
    subject: '',
    htmlBody: '',
    textBody: '',
  })
  const [errors, setErrors] = useState<FormErrors>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [preview, setPreview] = useState<{
    subject: string
    htmlBody: string
  } | null>(null)
  const [isPreviewing, setIsPreviewing] = useState(false)

  // Populate the form once the existing template has loaded.
  useEffect(() => {
    if (!data) return
    const active = data.versions.find(
      (version) => version.version === data.activeVersion
    )
    if (!active) return
    setValues({
      name: data.name,
      subject: active.subject,
      htmlBody: active.htmlBody,
      textBody: active.textBody,
    })
  }, [data])

  if (isLoading) {
    return <Spinner label="Loading template..." />
  }

  if (error) {
    return (
      <Alert tone="danger" title="Could not load template">
        {errorMessage(error)}
      </Alert>
    )
  }

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    const found = validate(values)
    setErrors(found)
    if (Object.keys(found).length > 0) {
      return
    }
    setSubmitError(null)
    setIsSaving(true)
    try {
      if (templateId === null) {
        await TemplateService.create(values)
      } else {
        await TemplateService.addVersion(templateId, values)
      }
      window.location.assign('/templates')
    } catch (err) {
      setSubmitError(errorMessage(err))
    } finally {
      setIsSaving(false)
    }
  }

  const onPreview = async () => {
    if (templateId === null) {
      return
    }
    setIsPreviewing(true)
    try {
      const result = await TemplateService.preview(templateId, {
        firstName: 'Sample',
      })
      setPreview({ subject: result.subject, htmlBody: result.htmlBody })
      setSubmitError(null)
    } catch (err) {
      setSubmitError(errorMessage(err))
    } finally {
      setIsPreviewing(false)
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-charcoal-900">
        {templateId === null ? 'New template' : 'Edit template'}
      </h1>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <form onSubmit={onSubmit} noValidate className="space-y-4">
            {submitError && (
              <Alert tone="danger" title="Could not save">
                {submitError}
              </Alert>
            )}
            <Input
              label="Name"
              value={values.name}
              error={errors.name}
              onChange={(event) =>
                setValues((prev) => ({ ...prev, name: event.target.value }))
              }
            />
            <Input
              label="Subject"
              value={values.subject}
              error={errors.subject}
              onChange={(event) =>
                setValues((prev) => ({
                  ...prev,
                  subject: event.target.value,
                }))
              }
            />
            <div className="space-y-1">
              <label
                htmlFor="htmlBody"
                className="block text-sm font-medium text-charcoal-900"
              >
                HTML body
              </label>
              <textarea
                id="htmlBody"
                rows={8}
                className="block w-full rounded-md border-gray-300 font-mono text-xs shadow-sm focus:border-navy-500 focus:ring-navy-500"
                value={values.htmlBody}
                aria-invalid={errors.htmlBody ? true : undefined}
                onChange={(event) =>
                  setValues((prev) => ({
                    ...prev,
                    htmlBody: event.target.value,
                  }))
                }
              />
              {errors.htmlBody && (
                <p className="text-sm text-red-600" role="alert">
                  {errors.htmlBody}
                </p>
              )}
            </div>
            <Textarea
              label="Text body"
              rows={5}
              value={values.textBody}
              error={errors.textBody}
              onChange={(event) =>
                setValues((prev) => ({
                  ...prev,
                  textBody: event.target.value,
                }))
              }
            />
            <div className="flex gap-2">
              <Button type="submit" isLoading={isSaving}>
                {templateId === null ? 'Create' : 'Save new version'}
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={onPreview}
                isLoading={isPreviewing}
                disabled={templateId === null}
              >
                Preview
              </Button>
            </div>
            {templateId === null && (
              <p className="text-xs text-gray-500">
                Preview is available after the first save.
              </p>
            )}
          </form>
        </Card>

        <Card>
          <h2 className="mb-4 text-sm font-medium text-gray-700">Preview</h2>
          {preview ? (
            <div className="space-y-4">
              <p className="text-sm font-medium text-charcoal-900">
                Subject: {preview.subject}
              </p>
              <div
                className="rounded border border-gray-200 p-4 text-sm"
                dangerouslySetInnerHTML={{ __html: preview.htmlBody }}
              />
            </div>
          ) : (
            <p className="py-12 text-center text-sm text-gray-500">
              Use the <Badge tone="info">Preview</Badge> button to render
              the template with sample recipient data.
            </p>
          )}
        </Card>
      </div>
    </div>
  )
}
