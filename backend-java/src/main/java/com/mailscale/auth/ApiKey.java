package com.mailscale.auth;

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
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "api_keys")
@EntityListeners(AuditingEntityListener.class)
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "key_hash", nullable = false, length = 128)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    @Column(name = "scopes", nullable = false, length = 500)
    private String scopes;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ApiKey() {
    }

    public ApiKey(Organization organization, String name, String keyHash, String keyPrefix,
                  String scopes, Instant expiresAt) {
        this.organization = organization;
        this.name = name;
        this.keyHash = keyHash;
        this.keyPrefix = keyPrefix;
        this.scopes = scopes;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getScopes() {
        return scopes;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markUsed(Instant now) {
        this.lastUsedAt = now;
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
    }
}
