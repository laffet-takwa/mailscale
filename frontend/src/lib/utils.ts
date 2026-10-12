/** Extracts a human-readable message from an unknown error. */
export function errorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message
  }
  if (typeof error === 'string') {
    return error
  }
  return 'An unexpected error occurred'
}

/** Formats an ISO timestamp for display. */
export function formatDate(iso: string | null): string {
  if (!iso) return '-'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString()
}

/** Formats a percentage value. */
export function formatPercent(value: number, digits = 1): string {
  return `${value.toFixed(digits)}%`
}

/** Joins class names, dropping falsy values. */
export function cx(
  ...classes: Array<string | false | null | undefined>
): string {
  return classes.filter(Boolean).join(' ')
}
