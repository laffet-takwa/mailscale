import { expect, test } from '@playwright/test'
import { login } from '../fixtures/helpers'

test.describe('AI assistant', () => {
  test('generates content using the mock provider', async ({ page }) => {
    await login(page)
    await page.goto('/ai-assistant')

    await page
      .getByLabel('Brief')
      .fill('Announce our new analytics dashboard to existing customers')

    await page.getByLabel('Product (optional)').fill('Analytics')
    await page.getByLabel('Audience (optional)').fill('customers')

    await page.getByRole('button', { name: 'Generate content' }).click()

    // The mock provider returns deterministically within seconds.
    await expect(page.getByText('Subject').first()).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByText('Provider: mock')).toBeVisible({
      timeout: 30_000,
    })
  })
})

test.describe('Email logs and status', () => {
  test('the transactional email logs render', async ({ page }) => {
    await login(page)
    await page.goto('/emails')

    await expect(
      page.getByRole('heading', { name: 'Email Logs' })
    ).toBeVisible()
    await expect(page.getByLabel('Status')).toBeVisible()
  })
})

test.describe('Suppressions', () => {
  test('a suppression entry can be added', async ({ page }) => {
    await login(page)
    await page.goto('/suppressions')

    const email = `suppressed-${Date.now()}@example.com`
    await page.getByLabel('Email').fill(email)
    await page.getByLabel('Reason').selectOption('MANUAL')
    await page.getByRole('button', { name: 'Add' }).click()

    await expect(page.getByText(email)).toBeVisible()
  })
})

test.describe('System health', () => {
  test('the monitoring page renders components', async ({ page }) => {
    await login(page)
    await page.goto('/monitoring')

    await expect(
      page.getByRole('heading', { name: 'System Health' })
    ).toBeVisible()
    await expect(page.getByText('Components')).toBeVisible()
  })
})
