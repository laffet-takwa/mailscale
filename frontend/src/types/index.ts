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

export interface Contact {
  id: number
  email: string
  firstName: string | null
  lastName: string | null
  status: 'ACTIVE' | 'UNSUBSCRIBED' | 'BOUNCED'
  consentStatus: 'GRANTED' | 'REVOKED' | 'PENDING'
  consentTimestamp: string | null
  unsubscribedAt: string | null
  attributes: string | null
  createdAt: string
  updatedAt: string
}

export interface ContactFormData {
  email: string
  firstName?: string
  lastName?: string
}

export interface PagedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface Signature {
  activeVersion: number
  createdAt: string
  id: number
  name: string
}

export interface TemplateVersion {
  id: number
  version: number
  subject: string
  htmlBody: string
  textBody: string
  variables: string
  createdAt: string
}

export interface TemplateDetail extends Signature {
  versions: TemplateVersion[]
}

export interface TemplateFormData {
  name: string
  subject: string
  htmlBody: string
  textBody: string
  variables?: string[]
}

export type CampaignStatus =
  | 'DRAFT'
  | 'SCHEDULED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'FAILED'

export interface Campaign {
  id: number
  name: string
  status: CampaignStatus
  segmentId: number | null
  templateId: number | null
  scheduledAt: string | null
  startedAt: string | null
  completedAt: string | null
  recipientsCount: number
  failureReason: string | null
  createdAt: string
  updatedAt: string
}

export interface CampaignRecipient {
  id: number
  campaignId: number
  contactId: number
  status:
    | 'PENDING'
    | 'QUEUED'
    | 'SENT'
    | 'DELIVERED'
    | 'BOUNCED'
    | 'FAILED'
    | 'SUPPRESSED'
    | 'SKIPPED'
  attempts: number
  lastAttemptAt: string | null
  deliveredAt: string | null
  errorCode: string | null
  createdAt: string
  updatedAt: string
}

export interface CampaignFormData {
  name: string
  segmentId: number
  templateId: number
}

export interface ValidationResult {
  valid: boolean
  problems: string[]
}

export interface Segment {
  id: number
  name: string
  definition: string
  createdAt: string
  updatedAt: string
}

export interface SegmentFormData {
  name: string
  definition: string
}

export interface ContactTag {
  id: number
  name: string
  createdAt: string
}

export interface DashboardStats {
  totalCampaigns: number
  queued: number
  sent: number
  bounced: number
  failed: number
  suppressed: number
  activeContacts: number
  deliveryRate: number
  sentPerDay: DailyPoint[]
}

export interface DailyPoint {
  date: string
  sent: number
}

export type EmailStatus =
  | 'QUEUED'
  | 'PROCESSING'
  | 'SENT'
  | 'DELIVERED'
  | 'BOUNCED'
  | 'FAILED'
  | 'SUPPRESSED'

export interface EmailMessage {
  id: string
  toEmail: string
  toName: string | null
  subject: string
  status: EmailStatus
  messageType: 'TRANSACTIONAL' | 'CAMPAIGN'
  templateId: number | null
  contactId: number | null
  campaignId: number | null
  correlationId: string
  idempotencyKey: string | null
  providerMessageId: string | null
  createdAt: string
  updatedAt: string
}

export interface QueuedEmail {
  messageId: string
  correlationId: string
  status: string
}

export interface SendEmailRequest {
  to: string
  toName?: string
  templateId: number
  variables?: Record<string, string>
  idempotencyKey: string
}

export type SuppressionReason =
  | 'UNSUBSCRIBE'
  | 'BOUNCE'
  | 'MANUAL'
  | 'COMPLAINT'

export interface SuppressionEntry {
  id: number
  email: string
  reason: SuppressionReason
  createdAt: string
}

export interface SuppressionFormData {
  email: string
  reason: SuppressionReason
}

export interface ApiKey {
  id: number
  name: string
  keyPrefix: string
  scopes: string
  lastUsedAt: string | null
  expiresAt: string | null
  revokedAt: string | null
  createdAt: string
}

export interface CreatedApiKey {
  id: number
  name: string
  secret: string
  scopes: string[]
  expiresAt: string | null
}

export interface GeneratedContent {
  subject: string
  htmlBody: string
  textBody: string
  callsToAction: string[]
  variants: Array<{ subject: string; previewText: string }>
  explanation: string
  provider: string
  latencyMs: number
}

export interface AiGenerationRequest {
  brief: string
  product?: string
  audience?: string
  tone?: 'PROFESSIONAL' | 'FRIENDLY' | 'URGENT' | 'MINIMAL'
  variantCount?: number
  includeCtas?: boolean
}

export interface CampaignSummary {
  id: number
  name: string
  status: string
  recipientsCount: number
  recipientsByStatus: Record<string, number>
}

export interface SystemStatus {
  overall: string
  components: Record<string, string>
}

export interface ApiError {
  message?: string
}
