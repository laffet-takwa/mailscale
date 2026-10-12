import client from './client'
import type {
  Campaign,
  CampaignFormData,
  CampaignRecipient,
  CampaignSummary,
  PagedResponse,
  EmailMessage,
  QueuedEmail,
  SendEmailRequest,
  Signature,
  TemplateDetail,
  TemplateFormData,
  ValidationResult,
} from '../types'

export const TemplateService = {
  async list(): Promise<Signature[]> {
    const response = await client.get<Signature[]>('/templates')
    return response.data
  },

  async get(id: number): Promise<TemplateDetail> {
    const response = await client.get<TemplateDetail>(`/templates/${id}`)
    return response.data
  },

  async create(data: TemplateFormData): Promise<Signature> {
    const response = await client.post<Signature>('/templates', data)
    return response.data
  },

  async addVersion(id: number, data: TemplateFormData): Promise<Signature> {
    const response = await client.put<Signature>(`/templates/${id}`, data)
    return response.data
  },

  async remove(id: number): Promise<void> {
    await client.delete(`/templates/${id}`)
  },

  async preview(
    id: number,
    variables: Record<string, string>
  ): Promise<{ subject: string; htmlBody: string; textBody: string }> {
    const response = await client.post(`/templates/${id}/preview`, variables)
    return response.data
  },
}

export const CampaignService = {
  async list(): Promise<Campaign[]> {
    const response = await client.get<Campaign[]>('/campaigns')
    return response.data
  },

  async get(id: number): Promise<Campaign> {
    const response = await client.get<Campaign>(`/campaigns/${id}`)
    return response.data
  },

  async create(data: CampaignFormData): Promise<Campaign> {
    const response = await client.post<Campaign>('/campaigns', data)
    return response.data
  },

  async validate(id: number): Promise<ValidationResult> {
    const response = await client.get<ValidationResult>(`/campaigns/${id}/validate`)
    return response.data
  },

  async schedule(id: number, scheduledAt: string): Promise<Campaign> {
    const response = await client.post<Campaign>(`/campaigns/${id}/schedule`, {
      scheduledAt,
    })
    return response.data
  },

  async dispatch(id: number): Promise<Campaign> {
    const response = await client.post<Campaign>(`/campaigns/${id}/dispatch`)
    return response.data
  },

  async cancel(id: number): Promise<Campaign> {
    const response = await client.post<Campaign>(`/campaigns/${id}/cancel`)
    return response.data
  },

  async recipients(id: number): Promise<CampaignRecipient[]> {
    const response = await client.get<CampaignRecipient[]>(`/campaigns/${id}/recipients`)
    return response.data
  },

  async summaries(): Promise<CampaignSummary[]> {
    const response = await client.get<CampaignSummary[]>('/analytics/campaigns')
    return response.data
  },
}

export const EmailService = {
  async send(data: SendEmailRequest): Promise<QueuedEmail> {
    const response = await client.post<QueuedEmail>('/emails', data)
    return response.data
  },

  async status(messageId: string): Promise<EmailMessage> {
    const response = await client.get<EmailMessage>(`/emails/${messageId}`)
    return response.data
  },

  async list(
    page = 0,
    size = 50,
    status?: EmailMessage['status']
  ): Promise<PagedResponse<EmailMessage>> {
    const response = await client.get<PagedResponse<EmailMessage>>('/emails', {
      params: { page, size, status },
    })
    return response.data
  },
}
