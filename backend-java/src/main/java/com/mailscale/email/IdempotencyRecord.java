package com.mailscale.email;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "idempotency_records", uniqueConstraints =
        @UniqueConstraint(name = "uk_idempotency_org_key", columnNames = {"organization_id", "idem_key"}))
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private com.mailscale.auth.Organization organization;

    @Column(name = "idem_key", nullable = false, length = 120)
    private String idemKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(com.mailscale.auth.Organization organization, String idemKey,
                               String requestHash, int responseStatus, String responseBody,
                               Instant expiresAt) {
        this.organization = organization;
        this.idemKey = idemKey;
        this.requestHash = requestHash;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public com.mailscale.auth.Organization getOrganization() {
        return organization;
    }

    public String getIdemKey() {
        return idemKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
