import client from './client'
import type {
  ApiKey,
  CreatedApiKey,
  CampaignSummary,
  DashboardStats,
  GeneratedContent,
  AiGenerationRequest,
  SuppressionEntry,
  SuppressionFormData,
  SystemStatus,
} from '../types'

export const AnalyticsService = {
  async dashboard(): Promise<DashboardStats> {
    const response = await client.get<DashboardStats>('/analytics/dashboard')
    return response.data
  },

  async campaignSummaries(): Promise<CampaignSummary[]> {
    const response = await client.get<CampaignSummary[]>('/analytics/campaigns')
    return response.data
  },
}

export const SuppressionService = {
  async list(): Promise<SuppressionEntry[]> {
    const response = await client.get<SuppressionEntry[]>('/suppressions')
    return response.data
  },

  async add(data: SuppressionFormData): Promise<SuppressionEntry> {
    const response = await client.post<SuppressionEntry>('/suppressions', data)
    return response.data
  },

  async remove(id: number): Promise<void> {
    await client.delete(`/suppressions/${id}`)
  },
}

export const ApiKeyService = {
  async list(): Promise<ApiKey[]> {
    const response = await client.get<ApiKey[]>('/auth/api-keys')
    return response.data
  },

  async create(name: string, expiresInDays?: number): Promise<CreatedApiKey> {
    const response = await client.post<CreatedApiKey>('/auth/api-keys', {
      name,
      expiresInDays,
    })
    return response.data
  },
}

export const AiService = {
  async generate(data: AiGenerationRequest): Promise<GeneratedContent> {
    const response = await client.post<GeneratedContent>('/ai/generate', data)
    return response.data
  },
}

export const MonitoringService = {
  async status(): Promise<SystemStatus> {
    const response = await client.get<SystemStatus>('/monitoring/status')
    return response.data
  },
}
