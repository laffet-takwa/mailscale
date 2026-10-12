import { useEffect, useState } from 'react'
import { authStore } from './authStore'
import type { AuthResponse } from '../types'

/**
 * React hook that exposes the current authenticated user.
 * Subscribes to the auth store so login/logout updates propagate.
 */
export function useAuth(): {
  user: AuthResponse | null
  isAuthenticated: boolean
  logout: () => void
} {
  const [user, setUser] = useState<AuthResponse | null>(
    () => authStore.getUser()
  )

  useEffect(() => {
    const unsubscribe = authStore.subscribe((next) => setUser(next))
    setUser(authStore.getUser())
    return unsubscribe
  }, [])

  return {
    user,
    isAuthenticated: authStore.isAuthenticated(),
    logout: () => authStore.logout(),
  }
}
