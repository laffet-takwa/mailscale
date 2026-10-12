import { expect, test } from '@playwright/test'
import { login, demoCredentials } from '../fixtures/helpers'

test.describe('Authentication', () => {
  test('a user can log in and reach the dashboard', async ({ page }) => {
    await page.goto('/login')

    await expect(
      page.getByRole('heading', { name: 'MailScale' })
    ).toBeVisible()

    await page.getByLabel('Email').fill(demoCredentials.email)
    await page.getByLabel('Password').fill(demoCredentials.password)
    await page.getByRole('button', { name: 'Sign in' }).click()

    await expect(page).toHaveURL(/dashboard/)
    await expect(
      page.getByRole('heading', { name: 'Dashboard' })
    ).toBeVisible()
  })

  test('invalid credentials show an error', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Email').fill('admin@mailscale.local')
    await page.getByLabel('Password').fill('wrong-password')
    await page.getByRole('button', { name: 'Sign in' }).click()

    await expect(
      page.getByRole('alert').first()
    ).toBeVisible()
  })

  test('the form validates before submitting', async ({ page }) => {
    await page.goto('/login')
    await page.getByRole('button', { name: 'Sign in' }).click()
    await expect(
      page.getByText('Enter a valid email address')
    ).toBeVisible()
  })
})

test.describe('Navigation', () => {
  test('every primary page is reachable', async ({ page }) => {
    await login(page)

    const routes: Array<[string, RegExp]> = [
      ['Contacts', /contacts/],
      ['Segments', /segments/],
      ['Templates', /templates/],
      ['Campaigns', /campaigns/],
      ['Email Logs', /emails/],
      ['Suppressions', /suppressions/],
      ['AI Assistant', /ai-assistant/],
      ['API Keys', /api-keys/],
      ['Settings', /settings/],
      ['System Health', /monitoring/],
    ]

    for (const [link, urlPattern] of routes) {
      await page.getByRole('link', { name: link, exact: true }).click()
      await expect(page).toHaveURL(urlPattern)
    }
  })
})
