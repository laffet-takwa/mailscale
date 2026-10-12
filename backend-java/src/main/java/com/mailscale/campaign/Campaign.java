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
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "campaigns")
@EntityListeners(AuditingEntityListener.class)
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private com.mailscale.auth.Organization organization;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    private com.mailscale.contact.Segment segment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private com.mailscale.template.Template template;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "recipients_count", nullable = false)
    private int recipientsCount;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Campaign() {
    }

    public Campaign(com.mailscale.auth.Organization organization, String name,
                    com.mailscale.contact.Segment segment, com.mailscale.template.Template template) {
        this.organization = organization;
        this.name = name;
        this.segment = segment;
        this.template = template;
    }

    public Long getId() {
        return id;
    }

    public com.mailscale.auth.Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public com.mailscale.contact.Segment getSegment() {
        return segment;
    }

    public com.mailscale.template.Template getTemplate() {
        return template;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public int getRecipientsCount() {
        return recipientsCount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void schedule(Instant scheduledAt) {
        assertTransition(CampaignStatus.DRAFT);
        this.scheduledAt = scheduledAt;
        this.status = CampaignStatus.SCHEDULED;
    }

    public void startProcessing(Instant now, int recipientsCount) {
        assertTransition(CampaignStatus.SCHEDULED);
        this.startedAt = now;
        this.recipientsCount = recipientsCount;
        this.status = CampaignStatus.PROCESSING;
    }

    public void complete(Instant now) {
        assertTransition(CampaignStatus.PROCESSING);
        this.completedAt = now;
        this.status = CampaignStatus.COMPLETED;
    }

    public void cancel(Instant now) {
        if (status == CampaignStatus.PROCESSING || status == CampaignStatus.SCHEDULED) {
            this.completedAt = now;
            this.status = CampaignStatus.CANCELLED;
            return;
        }
        throw new IllegalStateException("Campaign cannot be cancelled from status " + status);
    }

    public void fail(String reason) {
        if (status == CampaignStatus.PROCESSING || status == CampaignStatus.SCHEDULED) {
            this.failureReason = reason;
            this.completedAt = Instant.now();
            this.status = CampaignStatus.FAILED;
            return;
        }
        throw new IllegalStateException("Campaign cannot fail from status " + status);
    }

    private void assertTransition(CampaignStatus required) {
        if (status != required) {
            throw new IllegalStateException(
                    "Expected campaign status " + required + " but was " + status);
        }
    }
}
