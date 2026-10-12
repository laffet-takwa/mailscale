package com.mailscale.suppression;

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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "suppression_entries", uniqueConstraints =
        @UniqueConstraint(name = "uk_suppression_org_email", columnNames = {"organization_id", "email"}))
@EntityListeners(AuditingEntityListener.class)
public class SuppressionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private com.mailscale.auth.Organization organization;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private SuppressionReason reason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SuppressionEntry() {
    }

    public SuppressionEntry(com.mailscale.auth.Organization organization,
                              String email, SuppressionReason reason) {
        this.organization = organization;
        this.email = email;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public com.mailscale.auth.Organization getOrganization() {
        return organization;
    }

    public String getEmail() {
        return email;
    }

    public SuppressionReason getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
