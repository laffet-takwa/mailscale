import client from './client'
import type {
  AuthResponse,
  LoginCredentials,
  PagedResponse,
  Contact,
  ContactFormData,
  Segment,
  SegmentFormData,
  ContactTag,
} from '../types'

export const AuthService = {
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const response = await client.post<AuthResponse>('/auth/login', credentials)
    return response.data
  },

  async register(data: AuthResponse): Promise<AuthResponse> {
    const response = await client.post<AuthResponse>('/auth/register', data)
    return response.data
  },

  async me(): Promise<{ user: Record<string, unknown>; organization: Record<string, unknown> }> {
    const response = await client.get('/auth/me')
    return response.data
  },

  async createApiKey(name: string, expiresInDays?: number): Promise<{
    id: number
    name: string
    secret: string
    scopes: string[]
  }> {
    const response = await client.post('/auth/api-keys', { name, expiresInDays })
    return response.data
  },
}

export const ContactService = {
  async list(page = 0, size = 50): Promise<PagedResponse<Contact>> {
    const response = await client.get<PagedResponse<Contact>>('/contacts', {
      params: { page, size },
    })
    return response.data
  },

  async search(query: string, page = 0, size = 50): Promise<PagedResponse<Contact>> {
    const response = await client.get<PagedResponse<Contact>>('/contacts', {
      params: { q: query, page, size },
    })
    return response.data
  },

  async get(id: number): Promise<Contact> {
    const response = await client.get<Contact>(`/contacts/${id}`)
    return response.data
  },

  async create(data: ContactFormData): Promise<Contact> {
    const response = await client.post<Contact>('/contacts', data)
    return response.data
  },

  async update(id: number, data: ContactFormData): Promise<Contact> {
    const response = await client.put<Contact>(`/contacts/${id}`, data)
    return response.data
  },

  async remove(id: number): Promise<void> {
    await client.delete(`/contacts/${id}`)
  },

  async unsubscribe(id: number): Promise<Contact> {
    const response = await client.patch<Contact>(`/contacts/${id}/unsubscribe`)
    return response.data
  },

  async resubscribe(id: number): Promise<Contact> {
    const response = await client.patch<Contact>(`/contacts/${id}/resubscribe`)
    return response.data
  },

  async importCsv(csv: string): Promise<{
    processed: number
    duplicates: string[]
    errors: Array<{ row: number; message: string }>
  }> {
    const response = await client.post('/contacts/import', { csv })
    return response.data
  },
}

export const SegmentService = {
  async list(): Promise<Segment[]> {
    const response = await client.get<Segment[]>('/segments')
    return response.data
  },

  async create(data: SegmentFormData): Promise<Segment> {
    const response = await client.post<Segment>('/segments', data)
    return response.data
  },

  async update(id: number, data: SegmentFormData): Promise<Segment> {
    const response = await client.put<Segment>(`/segments/${id}`, data)
    return response.data
  },

  async remove(id: number): Promise<void> {
    await client.delete(`/segments/${id}`)
  },

  async evaluate(id: number, page = 0, size = 50): Promise<PagedResponse<Contact>> {
    const response = await client.get<PagedResponse<Contact>>(`/segments/${id}/evaluate`, {
      params: { page, size },
    })
    return response.data
  },
}

export const ContactTagService = {
  async list(): Promise<ContactTag[]> {
    const response = await client.get<ContactTag[]>('/contact-tags')
    return response.data
  },

  async create(name: string): Promise<ContactTag> {
    const response = await client.post<ContactTag>('/contact-tags', { name })
    return response.data
  },

  async remove(id: number): Promise<void> {
    await client.delete(`/contact-tags/${id}`)
  },
}
