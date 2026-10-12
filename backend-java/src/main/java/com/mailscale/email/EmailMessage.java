package com.mailscale.email;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "email_messages", indexes = {
        @Index(name = "idx_email_messages_org_created", columnList = "organization_id, created_at DESC"),
        @Index(name = "idx_email_messages_correlation", columnList = "correlation_id")
})
@EntityListeners(AuditingEntityListener.class)
public class EmailMessage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private com.mailscale.auth.Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    @Column(name = "to_email", nullable = false, length = 320)
    private String toEmail;

    @Column(name = "to_name", length = 200)
    private String toName;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "html_body", columnDefinition = "TEXT")
    private String htmlBody;

    @Column(name = "text_body", columnDefinition = "TEXT")
    private String textBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MessageStatus status = MessageStatus.QUEUED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private com.mailscale.template.Template template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private com.mailscale.contact.Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private com.mailscale.campaign.Campaign campaign;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EmailMessage() {
    }

    public EmailMessage(UUID id, com.mailscale.auth.Organization organization,
                        MessageType messageType, String toEmail, String toName,
                        String subject, String htmlBody, String textBody,
                        com.mailscale.template.Template template,
                        com.mailscale.contact.Contact contact,
                        com.mailscale.campaign.Campaign campaign,
                        String correlationId, String idempotencyKey) {
        this.id = id;
        this.organization = organization;
        this.messageType = messageType;
        this.toEmail = toEmail;
        this.toName = toName;
        this.subject = subject;
        this.htmlBody = htmlBody;
        this.textBody = textBody;
        this.template = template;
        this.contact = contact;
        this.campaign = campaign;
        this.correlationId = correlationId;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getId() {
        return id;
    }

    @JsonIgnore
    public com.mailscale.auth.Organization getOrganization() {
        return organization;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getToEmail() {
        return toEmail;
    }

    public String getToName() {
        return toName;
    }

    public String getSubject() {
        return subject;
    }

    @JsonIgnore
    public String getHtmlBody() {
        return htmlBody;
    }

    @JsonIgnore
    public String getTextBody() {
        return textBody;
    }

    public MessageStatus getStatus() {
        return status;
    }

    @JsonIgnore
    public com.mailscale.template.Template getTemplate() {
        return template;
    }

    @JsonIgnore
    public com.mailscale.contact.Contact getContact() {
        return contact;
    }

    @JsonIgnore
    public com.mailscale.campaign.Campaign getCampaign() {
        return campaign;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markProcessing() {
        if (status == MessageStatus.QUEUED) {
            this.status = MessageStatus.PROCESSING;
        }
    }

    public void markSent(String providerMessageId) {
        if (status == MessageStatus.PROCESSING || status == MessageStatus.QUEUED) {
            this.providerMessageId = providerMessageId;
            this.status = MessageStatus.SENT;
        }
    }

    public void markDelivered(String providerMessageId) {
        if (status != MessageStatus.FAILED && status != MessageStatus.SUPPRESSED) {
            this.providerMessageId = providerMessageId;
            this.status = MessageStatus.DELIVERED;
        }
    }

    public void markBounced(String providerMessageId) {
        if (status != MessageStatus.FAILED) {
            this.providerMessageId = providerMessageId;
            this.status = MessageStatus.BOUNCED;
        }
    }

    public void markFailed() {
        if (!isTerminal()) {
            this.status = MessageStatus.FAILED;
        }
    }

    public void markSuppressed() {
        if (status == MessageStatus.QUEUED || status == MessageStatus.PROCESSING) {
            this.status = MessageStatus.SUPPRESSED;
        }
    }

    public boolean isTerminal() {
        return status == MessageStatus.DELIVERED
                || status == MessageStatus.FAILED
                || status == MessageStatus.SUPPRESSED
                || status == MessageStatus.BOUNCED;
    }

    public static UUID deriveId(String idempotencyKey, String organizationSlug) {
        return UUID.nameUUIDFromBytes(
                ("mailscale:" + organizationSlug + ":" + idempotencyKey).getBytes(
                        java.nio.charset.StandardCharsets.UTF_8));
    }
}
