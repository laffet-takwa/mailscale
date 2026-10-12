import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach, vi } from 'vitest'

/**
 * jsdom does not implement PointerEvent, which
 * @testing-library/user-event v14 requires for click/type
 * interactions.
 */
if (
  typeof window !== 'undefined' &&
  typeof window.PointerEvent === 'undefined'
) {
  class PointerEventPolyfill extends MouseEvent {
    public pointerId: number
    public pointerType: string

    constructor(type: string, params: PointerEventInit = {}) {
      super(type, params)
      this.pointerId = params.pointerId ?? 1
      this.pointerType = params.pointerType ?? 'mouse'
    }
  }

  Object.defineProperty(window, 'PointerEvent', {
    value: PointerEventPolyfill,
    writable: true,
  })
}

/**
 * React 18 requires this flag so that updates from event handlers
 * are wrapped in act().
 */
const g = globalThis as unknown as { IS_REACT_ACT_ENVIRONMENT?: boolean }
g.IS_REACT_ACT_ENVIRONMENT = true

beforeEach(() => {
  vi.stubGlobal('matchMedia', (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }))
})

afterEach(() => {
  cleanup()
  localStorage.clear()
})
