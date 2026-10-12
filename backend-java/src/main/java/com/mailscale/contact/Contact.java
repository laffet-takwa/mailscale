package com.mailscale.contact;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "contacts", uniqueConstraints =
        @UniqueConstraint(name = "uk_contacts_org_email", columnNames = {"organization_id", "email"}))
@EntityListeners(AuditingEntityListener.class)
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private com.mailscale.auth.Organization organization;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "first_name", length = 120)
    private String firstName;

    @Column(name = "last_name", length = 120)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContactStatus status = ContactStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status", nullable = false, length = 20)
    private ConsentStatus consentStatus = ConsentStatus.GRANTED;

    @Column(name = "consent_timestamp")
    private Instant consentTimestamp;

    @Column(name = "unsubscribed_at")
    private Instant unsubscribedAt;

    @Column(name = "attributes", columnDefinition = "TEXT")
    private String attributes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Contact() {
    }

    public Contact(com.mailscale.auth.Organization organization, String email,
                   String firstName, String lastName, String attributes) {
        this.organization = organization;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.attributes = attributes;
        this.consentTimestamp = Instant.now();
    }

    public Long getId() {
        return id;
    }

    @JsonIgnore
    public com.mailscale.auth.Organization getOrganization() {
        return organization;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public ContactStatus getStatus() {
        return status;
    }

    public ConsentStatus getConsentStatus() {
        return consentStatus;
    }

    public Instant getConsentTimestamp() {
        return consentTimestamp;
    }

    public Instant getUnsubscribedAt() {
        return unsubscribedAt;
    }

    public String getAttributes() {
        return attributes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String firstName, String lastName, String attributes) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.attributes = attributes;
    }

    public void unsubscribe(Instant now) {
        this.status = ContactStatus.UNSUBSCRIBED;
        this.unsubscribedAt = now;
    }

    public void resubscribe(Instant now) {
        this.status = ContactStatus.ACTIVE;
        this.unsubscribedAt = null;
        this.consentStatus = ConsentStatus.GRANTED;
        this.consentTimestamp = now;
    }

    public void markBounced() {
        this.status = ContactStatus.BOUNCED;
    }

    public boolean isDeliverable() {
        return status == ContactStatus.ACTIVE
                && consentStatus == ConsentStatus.GRANTED
                && unsubscribedAt == null;
    }
}
