import { useMemo, useState } from 'react'
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
import { ContactService } from '../api/contacts'
import type { Contact } from '../types'
import { errorMessage } from '../lib/utils'

const STATUS_TONES: Record<Contact['status'], 'success' | 'warning' | 'danger'> = {
  ACTIVE: 'success',
  UNSUBSCRIBED: 'warning',
  BOUNCED: 'danger',
}

function createColumns(
  onUnsubscribe: (contact: Contact) => void,
  onResubscribe: (contact: Contact) => void,
  onDelete: (contact: Contact) => void,
  pendingIds: number[]
): Array<Column<Contact>> {
  return [
    {
      key: 'email',
      header: 'Email',
      render: (contact) => (
        <span className="font-medium text-navy-700">{contact.email}</span>
      ),
    },
    {
      key: 'name',
      header: 'Name',
      render: (contact) =>
        [contact.firstName, contact.lastName].filter(Boolean).join(' ') || '-',
    },
    {
      key: 'status',
      header: 'Status',
      render: (contact) => (
        <Badge tone={STATUS_TONES[contact.status]}>{contact.status}</Badge>
      ),
    },
    {
      key: 'consent',
      header: 'Consent',
      render: (contact) => (
        <Badge
          tone={contact.consentStatus === 'GRANTED' ? 'success' : 'warning'}
        >
          {contact.consentStatus}
        </Badge>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (contact) => (
        <div className="flex gap-2">
          {contact.status === 'ACTIVE' ? (
            <Button
              variant="secondary"
              onClick={() => onUnsubscribe(contact)}
              disabled={pendingIds.includes(contact.id)}
            >
              Unsubscribe
            </Button>
          ) : (
            <Button
              variant="secondary"
              onClick={() => onResubscribe(contact)}
              disabled={pendingIds.includes(contact.id)}
            >
              Resubscribe
            </Button>
          )}
          <Button
            variant="danger"
            onClick={() => onDelete(contact)}
            disabled={pendingIds.includes(contact.id)}
          >
            Delete
          </Button>
        </div>
      ),
    },
  ]
}

/** Contacts list with search, creation, unsubscribe, and deletion. */
export default function ContactsPage() {
  const [search, setSearch] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data, isLoading, error } = useQuery({
    queryKey: ['contacts'],
    queryFn: () => ContactService.list(0, 50),
  })

  const unsubscribe = useMutation({
    mutationFn: (contact: Contact) => ContactService.unsubscribe(contact.id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['contacts'] })
    },
  })

  const resubscribe = useMutation({
    mutationFn: (contact: Contact) => ContactService.resubscribe(contact.id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['contacts'] })
    },
  })

  const remove = useMutation({
    mutationFn: (contact: Contact) => ContactService.remove(contact.id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['contacts'] })
    },
  })

  const pendingIds = [
    ...unsubscribe.variables ? [unsubscribe.variables.id] : [],
    ...resubscribe.variables ? [resubscribe.variables.id] : [],
    ...remove.variables ? [remove.variables.id] : [],
  ]

  const columns = createColumns(
    (contact) => unsubscribe.mutate(contact),
    (contact) => resubscribe.mutate(contact),
    (contact) => remove.mutate(contact),
    pendingIds
  )

  // Client-side filtering for the current page: O(n) over the page
  // of at most 50 contacts; large lists are paginated server-side.
  const contacts = useMemo(() => {
    const source = data?.content ?? []
    const needle = search.trim().toLowerCase()
    if (!needle) return source
    return source.filter((contact) =>
      contact.email.toLowerCase().includes(needle)
    )
  }, [data, search])

  if (isLoading) {
    return <Spinner label="Loading contacts..." />
  }

  if (error) {
    return (
      <Alert tone="danger" title="Could not load contacts">
        {errorMessage(error)}
      </Alert>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-charcoal-900">Contacts</h1>
        <Button onClick={() => setShowForm((value) => !value)}>
          {showForm ? 'Close' : 'Add contact'}
        </Button>
      </div>

      {showForm && (
        <Card>
          <ContactForm
            error={formError}
            onSubmit={async (values) => {
              setFormError(null)
              try {
                await ContactService.create(values)
                setShowForm(false)
                void queryClient.invalidateQueries({ queryKey: ['contacts'] })
              } catch (err) {
                setFormError(errorMessage(err))
              }
            }}
          />
        </Card>
      )}

      <Card>
        <div className="mb-4 max-w-sm">
          <Input
            label="Filter"
            placeholder="Filter by email"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
        <DataTable
          columns={columns}
          rows={contacts}
          keyFn={(contact) => contact.id}
          emptyMessage="No contacts yet. Add your first contact above."
        />
      </Card>
    </div>
  )
}

function ContactForm({
  onSubmit,
  error,
}: {
  onSubmit: (values: { email: string; firstName?: string; lastName?: string }) => void
  error: string | null
}) {
  const [email, setEmail] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')

  return (
    <form
      className="space-y-4"
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit({
          email,
          firstName: firstName || undefined,
          lastName: lastName || undefined,
        })
      }}
    >
      {error && (
        <Alert tone="danger" title="Could not save">
          {error}
        </Alert>
      )}
      <Input
        label="Email"
        type="email"
        name="email"
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        required
      />
      <Input
        label="First name"
        name="firstName"
        value={firstName}
        onChange={(event) => setFirstName(event.target.value)}
      />
      <Input
        label="Last name"
        name="lastName"
        value={lastName}
        onChange={(event) => setLastName(event.target.value)}
      />
      <div className="flex gap-2">
        <Button type="submit">Save contact</Button>
      </div>
    </form>
  )
}
