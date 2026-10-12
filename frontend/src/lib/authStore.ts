import AuthService from '../api/auth'
import type { AuthResponse, LoginCredentials, RegisterData } from '../api/types'

const TOKEN_KEY = 'mailscale_token'
const USER_KEY = 'mailscale_user'

export class AuthStore {
  private token: string | null = null
  private user: AuthResponse | null = null
  private listeners: Array<(user: AuthResponse | null) => void> = []

  constructor() {
    this.token = localStorage.getItem(TOKEN_KEY)
    const stored = localStorage.getItem(USER_KEY)
    if (stored) {
      try {
        this.user = JSON.parse(stored)
      } catch {
        localStorage.removeItem(USER_KEY)
      }
    }
  }

  getToken(): string | null {
    return this.token
  }

  getUser(): AuthResponse | null {
    return this.user
  }

  isAuthenticated(): boolean {
    return this.token !== null
  }

  subscribe(listener: (user: AuthResponse | null) => void): () => void {
    this.listeners.push(listener)
    return () => {
      this.listeners = this.listeners.filter((l) => l !== listener)
    }
  }

  private notify(): void {
    this.listeners.forEach((listener) => listener(this.user))
  }

  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const response = await AuthService.login(credentials)
    this.persist(response)
    this.notify()
    return response
  }

  async register(data: RegisterData): Promise<AuthResponse> {
    const response = await AuthService.register(data)
    this.persist(response)
    this.notify()
    return response
  }

  logout(): void {
    this.token = null
    this.user = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    this.notify()
  }

  private persist(response: AuthResponse): void {
    this.token = response.token
    this.user = response
    localStorage.setItem(TOKEN_KEY, response.token)
    localStorage.setItem(USER_KEY, JSON.stringify(response))
  }
}

export const authStore = new AuthStore()
