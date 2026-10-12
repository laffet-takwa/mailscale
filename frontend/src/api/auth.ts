import client from './client'
import type { AuthResponse, LoginCredentials, RegisterData } from './types'

class AuthService {
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const response = await client.post<AuthResponse>('/auth/login', credentials)
    return response.data
  }

  async register(data: RegisterData): Promise<AuthResponse> {
    const response = await client.post<AuthResponse>('/auth/register', data)
    return response.data
  }
}

export default new AuthService()
