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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "contact_tags", uniqueConstraints =
        @UniqueConstraint(name = "uk_contact_tags_org_name",
                columnNames = {"organization_id", "name"}))
@EntityListeners(AuditingEntityListener.class)
public class ContactTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private com.mailscale.auth.Organization organization;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ContactTag() {
    }

    public ContactTag(com.mailscale.auth.Organization organization, String name) {
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
