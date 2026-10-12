import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  Alert,
  Badge,
  Button,
  Card,
  EmptyState,
  Spinner,
} from '../components/ui'
import { DataTable } from '../components/DataTable'
import type { Column } from '../components/DataTable'
import { TemplateService } from '../api/campaigns'
import type { Signature } from '../types'
import { errorMessage, formatDate } from '../lib/utils'

/** Templates list with a link into the editor/preview. */
export default function TemplatesPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['templates'],
    queryFn: () => TemplateService.list(),
  })

  const columns: Array<Column<Signature>> = [
    {
      key: 'name',
      header: 'Name',
      render: (template) => (
        <Link
          to={`/templates/${template.id}`}
          className="font-medium text-navy-700 hover:text-navy-800"
        >
          {template.name}
        </Link>
      ),
    },
    {
      key: 'activeVersion',
      header: 'Active version',
      render: (template) => (
        <Badge tone="info">v{template.activeVersion}</Badge>
      ),
    },
    {
      key: 'createdAt',
      header: 'Created',
      render: (template) => formatDate(template.createdAt),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (template) => (
        <Link to={`/templates/${template.id}`}>
          <Button variant="secondary">Edit</Button>
        </Link>
      ),
    },
  ]

  if (isLoading) {
    return <Spinner label="Loading templates..." />
  }

  if (error) {
    return (
      <Alert tone="danger" title="Could not load templates">
        {errorMessage(error)}
      </Alert>
    )
  }

  const templates = data ?? []

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-charcoal-900">Templates</h1>
        <Link to="/templates/new">
          <Button>New template</Button>
        </Link>
      </div>

      <Card>
        {templates.length === 0 ? (
          <EmptyState
            title="No templates yet"
            description="Templates power campaigns and transactional emails."
            action={
              <Link to="/templates/new">
                <Button>Create template</Button>
              </Link>
            }
          />
        ) : (
          <DataTable
            columns={columns}
            rows={templates}
            keyFn={(template) => template.id}
          />
        )}
      </Card>
    </div>
  )
}
