import { useEffect, useRef, useState } from 'react'

/** Debounces a rapidly-changing value (used for search inputs). */
export function useDebounce<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(timer)
  }, [value, delayMs])

  return debounced
}

/** Focus management helper for dialogs and forms. */
export function useAutoFocus<T extends HTMLElement>() {
  const ref = useRef<T>(null)
  useEffect(() => {
    ref.current?.focus()
  }, [])
  return ref
}
