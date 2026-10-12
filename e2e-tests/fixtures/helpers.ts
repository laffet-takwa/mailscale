import { expect, test } from '@playwright/test'

/**
 * Shared helpers for the MailScale e2e suite.
 *
 * Credentials match the demo data seeded by the API when
 * mailscale.demo.seed-enabled=true (the default).
 */
const BASE = process.env.MAILSCALE_API ?? 'http://localhost:8080'

export const demoCredentials = {
  email: 'admin@mailscale.local',
  password: 'demo1234',
}

export async function login(page: import('@playwright/test').Page) {
  await page.goto('/login')
  await page.getByLabel('Email').fill(demoCredentials.email)
  await page.getByLabel('Password').fill(demoCredentials.password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page).toHaveURL(/dashboard/)
}

/** Obtains a JWT directly from the API for tests that call the HTTP layer. */
export async function apiToken(): Promise<string> {
  const response = await fetch(`${BASE}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(demoCredentials),
  })
  if (!response.ok) {
    throw new Error(`Login failed with status ${response.status}`)
  }
  const body = (await response.json()) as { token: string }
  return body.token
}
