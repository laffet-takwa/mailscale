import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import LoginPage from '../pages/LoginPage'

function renderLogin() {
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>
  )
}

describe('LoginPage', () => {
  it('renders the sign-in form', () => {
    renderLogin()
    expect(
      screen.getByRole('heading', { name: 'MailScale' })
    ).toBeInTheDocument()
    expect(
      screen.getByRole('textbox', { name: 'Email' })
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Sign in' })
    ).toBeInTheDocument()
  })

  it('validates a malformed email', async () => {
    renderLogin()
    await userEvent.type(
      screen.getByRole('textbox', { name: 'Email' }),
      'not-an-email'
    )
    await userEvent.type(screen.getByLabelText('Password'), 'password123')
    await userEvent.click(
      screen.getByRole('button', { name: 'Sign in' })
    )
    expect(
      await screen.findByText('Enter a valid email address')
    ).toBeInTheDocument()
  })

  it('rejects a short password', async () => {
    renderLogin()
    await userEvent.type(
      screen.getByRole('textbox', { name: 'Email' }),
      'user@example.com'
    )
    await userEvent.type(screen.getByLabelText('Password'), 'short')
    await userEvent.click(
      screen.getByRole('button', { name: 'Sign in' })
    )
    expect(
      await screen.findByText(
        'Password must be at least 8 characters'
      )
    ).toBeInTheDocument()
  })

  it('links to the registration page', () => {
    renderLogin()
    const link = screen.getByRole('link', { name: 'Register' })
    expect(link).toHaveAttribute('href', '/register')
  })
})
