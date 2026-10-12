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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "template_versions", uniqueConstraints =
        @UniqueConstraint(name = "uk_template_versions", columnNames = {"template_id", "version"}))
@EntityListeners(AuditingEntityListener.class)
public class TemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "html_body", nullable = false, columnDefinition = "TEXT")
    private String htmlBody;

    @Column(name = "text_body", nullable = false, columnDefinition = "TEXT")
    private String textBody;

    @Column(name = "variables", columnDefinition = "TEXT")
    private String variables;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TemplateVersion() {
    }

    public TemplateVersion(Template template, int version, String subject,
                           String htmlBody, String textBody, String variables) {
        this.template = template;
        this.version = version;
        this.subject = subject;
        this.htmlBody = htmlBody;
        this.textBody = textBody;
        this.variables = variables;
    }

    public Long getId() {
        return id;
    }

    @JsonIgnore
    public Template getTemplate() {
        return template;
    }

    public int getVersion() {
        return version;
    }

    public String getSubject() {
        return subject;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public String getTextBody() {
        return textBody;
    }

    public String getVariables() {
        return variables;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
