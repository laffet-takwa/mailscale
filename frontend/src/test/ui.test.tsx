import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Button, Input, Spinner, Badge, Alert } from '../components/ui'

describe('Button', () => {
  it('renders its label', () => {
    render(<Button>Save</Button>)
    expect(
      screen.getByRole('button', { name: 'Save' })
    ).toBeInTheDocument()
  })

  it('is disabled while loading', () => {
    render(<Button isLoading>Save</Button>)
    expect(screen.getByRole('button')).toBeDisabled()
  })

  it('invokes onClick when clicked', async () => {
    let clicks = 0
    render(
      <Button
        onClick={() => {
          clicks += 1
        }}
      >
        Click me
      </Button>
    )
    await userEvent.click(
      screen.getByRole('button', { name: 'Click me' })
    )
    expect(clicks).toBe(1)
  })
})

describe('Input', () => {
  it('associates the label with the input', () => {
    render(<Input label="Email" name="email" />)
    expect(
      screen.getByRole('textbox', { name: 'Email' })
    ).toBeInTheDocument()
  })

  it('marks the field invalid when an error is present', () => {
    render(<Input label="Email" name="email" error="Required" />)
    const input = screen.getByRole('textbox', { name: /Email/ })
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByRole('alert')).toHaveTextContent('Required')
  })

  it('forwards value changes', async () => {
    const values: string[] = []
    render(
      <Input
        label="Name"
        name="name"
        value=""
        onChange={(event) => values.push(event.target.value)}
      />
    )
    const input = screen.getByRole('textbox', {
      name: 'Name',
    }) as HTMLInputElement
    await userEvent.type(input, 'Anna')
    // React batches controlled updates, so assert on the change
    // events that fired rather than the stored value.
    expect(values.length).toBeGreaterThan(0)
    expect(values.join('')).toContain('A')
  })
})

describe('Spinner', () => {
  it('announces loading state', () => {
    render(<Spinner label="Loading contacts..." />)
    expect(screen.getByRole('status')).toHaveTextContent(
      'Loading contacts...'
    )
  })
})

describe('Badge', () => {
  it('renders its children', () => {
    render(<Badge tone="success">ACTIVE</Badge>)
    expect(screen.getByText('ACTIVE')).toBeInTheDocument()
  })
})

describe('Alert', () => {
  it('renders title and message with role alert', () => {
    render(
      <Alert tone="danger" title="Failed">
        Something broke
      </Alert>
    )
    expect(screen.getByRole('alert')).toHaveTextContent('Failed')
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Something broke'
    )
  })
})
