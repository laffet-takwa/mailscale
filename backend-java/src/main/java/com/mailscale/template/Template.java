package com.mailscale.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "templates", uniqueConstraints =
        @UniqueConstraint(name = "uk_templates_org_name",
                columnNames = {"organization_id", "name"}))
@EntityListeners(AuditingEntityListener.class)
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private com.mailscale.auth.Organization organization;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "active_version", nullable = false)
    private int activeVersion = 1;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Template() {
    }

    public Template(com.mailscale.auth.Organization organization, String name) {
        this.organization = organization;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    @JsonIgnore
    public com.mailscale.auth.Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    public int getActiveVersion() {
        return activeVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void bumpVersion() {
        this.activeVersion++;
    }
}
