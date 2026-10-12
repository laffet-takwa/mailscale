import { describe, expect, it } from 'vitest'
import { errorMessage, formatDate, formatPercent, cx } from '../lib/utils'

describe('utils', () => {
  describe('errorMessage', () => {
    it('extracts message from Error instances', () => {
      expect(errorMessage(new Error('boom'))).toBe('boom')
    })

    it('returns string errors verbatim', () => {
      expect(errorMessage('failure')).toBe('failure')
    })

    it('falls back for unknown types', () => {
      expect(errorMessage(null)).toBe('An unexpected error occurred')
      expect(errorMessage(42)).toBe('An unexpected error occurred')
    })
  })

  describe('formatDate', () => {
    it('formats ISO timestamps', () => {
      const result = formatDate('2026-01-15T10:30:00Z')
      expect(result).not.toBe('-')
      expect(result.length).toBeGreaterThan(0)
    })

    it('returns a dash for null or invalid dates', () => {
      expect(formatDate(null)).toBe('-')
      expect(formatDate('not-a-date')).toBe('-')
    })
  })

  describe('formatPercent', () => {
    it('formats with one decimal by default', () => {
      expect(formatPercent(95.25)).toBe('95.3%')
    })

    it('honours the digits parameter', () => {
      expect(formatPercent(95.25, 2)).toBe('95.25%')
    })
  })

  describe('cx', () => {
    it('joins truthy class names', () => {
      expect(cx('a', 'b', 'c')).toBe('a b c')
    })

    it('drops falsy values', () => {
      expect(cx('a', false, undefined, null, 'b')).toBe('a b')
    })

    it('returns an empty string with no classes', () => {
      expect(cx()).toBe('')
    })
  })
})
