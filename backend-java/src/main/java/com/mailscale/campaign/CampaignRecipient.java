package com.mailscale.campaign;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "campaign_recipients", uniqueConstraints =
        @UniqueConstraint(name = "uk_campaign_recipients", columnNames = {"campaign_id", "contact_id"}))
@EntityListeners(AuditingEntityListener.class)
public class CampaignRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private com.mailscale.contact.Contact contact;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecipientStatus status = RecipientStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "error_code", length = 60)
    private String errorCode;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CampaignRecipient() {
    }

    public CampaignRecipient(Campaign campaign, com.mailscale.contact.Contact contact) {
        this.campaign = campaign;
        this.contact = contact;
    }

    public Long getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public com.mailscale.contact.Contact getContact() {
        return contact;
    }

    public RecipientStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markQueued(Instant now) {
        this.status = RecipientStatus.QUEUED;
        this.lastAttemptAt = now;
    }

    public void markDelivered(Instant now, String providerMessageId) {
        this.status = RecipientStatus.DELIVERED;
        this.deliveredAt = now;
        this.errorCode = null;
    }

    public void markFailed(Instant now, String errorCode) {
        this.status = RecipientStatus.FAILED;
        this.errorCode = errorCode;
    }

    public void markSuppressed() {
        this.status = RecipientStatus.SUPPRESSED;
        this.errorCode = "SUPPRESSED";
    }

    public void markSkipped(String reason) {
        this.status = RecipientStatus.SKIPPED;
        this.errorCode = reason;
    }

    public void incrementAttempt(Instant now) {
        this.attempts++;
        this.lastAttemptAt = now;
    }
}
