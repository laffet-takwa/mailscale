package com.mailscale.email;

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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "delivery_attempts")
@EntityListeners(AuditingEntityListener.class)
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private EmailMessage message;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(name = "smtp_code", length = 10)
    private String smtpCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DeliveryAttempt() {
    }

    public DeliveryAttempt(EmailMessage message, int attemptNumber, String provider,
                             DeliveryStatus status, String smtpCode,
                             String errorMessage, Integer durationMs) {
        this.message = message;
        this.attemptNumber = attemptNumber;
        this.provider = provider;
        this.status = status;
        this.smtpCode = smtpCode;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
    }

    public Long getId() {
        return id;
    }

    public EmailMessage getMessage() {
        return message;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public String getProvider() {
        return provider;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public String getSmtpCode() {
        return smtpCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
