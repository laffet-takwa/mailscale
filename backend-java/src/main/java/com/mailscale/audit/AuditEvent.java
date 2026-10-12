package com.mailscale.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_events_org_created", columnList = "organization_id, created_at DESC")
})
@EntityListeners(AuditingEntityListener.class)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private com.mailscale.auth.Organization organization;

    @Column(name = "actor_email", length = 320)
    private String actorEmail;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(com.mailscale.auth.Organization organization, String actorEmail,
                        String action, String entityType, String entityId, String metadata) {
        this.organization = organization;
        this.actorEmail = actorEmail;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.metadata = metadata;
    }

    public Long getId() {
        return id;
    }

    public com.mailscale.auth.Organization getOrganization() {
        return organization;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
