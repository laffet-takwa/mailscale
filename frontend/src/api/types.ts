export interface AuthResponse {
  token: string
  userId: number
  organizationId: number
  organizationSlug: string
  role: string
}

export interface LoginCredentials {
  email: string
  password: string
}

export interface RegisterData {
  email: string
  password: string
  fullName: string
  organizationName: string
  organizationSlug: string
}
