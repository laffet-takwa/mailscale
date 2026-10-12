package com.mailscale.contact;

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
@Table(name = "segments", uniqueConstraints =
        @UniqueConstraint(name = "uk_segments_org_name",
                columnNames = {"organization_id", "name"}))
@EntityListeners(AuditingEntityListener.class)
public class Segment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private com.mailscale.auth.Organization organization;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "definition", nullable = false, columnDefinition = "TEXT")
    private String definition;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Segment() {
    }

    public Segment(com.mailscale.auth.Organization organization,
                   String name, String definition) {
        this.organization = organization;
        this.name = name;
        this.definition = definition;
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

    public String getDefinition() {
        return definition;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
