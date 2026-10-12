import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { EmptyState, Card } from '../components/ui'
import { DataTable } from '../components/DataTable'
import type { Column } from '../components/DataTable'

interface Row {
  id: number
  name: string
}

const COLUMNS: Array<Column<Row>> = [
  { key: 'name', header: 'Name', render: (row) => row.name },
]

describe('DataTable', () => {
  it('renders rows with headers', () => {
    render(
      <DataTable
        columns={COLUMNS}
        rows={[{ id: 1, name: 'Anna' }]}
        keyFn={(row) => row.id}
      />
    )
    expect(screen.getByRole('table')).toBeInTheDocument()
    expect(
      screen.getByRole('columnheader', { name: 'Name' })
    ).toBeInTheDocument()
    expect(screen.getByRole('cell')).toHaveTextContent('Anna')
  })

  it('shows an empty message with no rows', () => {
    render(
      <DataTable
        columns={COLUMNS}
        rows={[]}
        keyFn={(row) => row.id}
        emptyMessage="No contacts found"
      />
    )
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(screen.getByText('No contacts found')).toBeInTheDocument()
  })
})

describe('EmptyState', () => {
  it('renders title and description', () => {
    render(
      <EmptyState
        title="Nothing here"
        description="Create something first"
      />
    )
    expect(screen.getByText('Nothing here')).toBeInTheDocument()
    expect(
      screen.getByText('Create something first')
    ).toBeInTheDocument()
  })
})

describe('Card', () => {
  it('renders children', () => {
    render(<Card>Content</Card>)
    expect(screen.getByText('Content')).toBeInTheDocument()
  })
})
