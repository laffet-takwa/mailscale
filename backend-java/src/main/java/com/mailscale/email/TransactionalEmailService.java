package com.mailscale.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailscale.auth.Organization;
import com.mailscale.common.ConflictException;
import com.mailscale.common.NotFoundException;
import com.mailscale.contact.Contact;
import com.mailscale.contact.ContactRepository;
import com.mailscale.outbox.Outbox;
import com.mailscale.suppression.SuppressionEntryRepository;
import com.mailscale.template.Template;
import com.mailscale.template.TemplateRepository;
import com.mailscale.template.TemplateRenderer;
import com.mailscale.template.TemplateVersion;
import com.mailscale.template.TemplateVersionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accepts transactional email requests, enforces idempotency, validates
 * the recipient against suppression/consent state, persists the message
 * and an outbox event in one transaction, and returns 202.
 */
@Service
public class TransactionalEmailService {

    private static final Logger log = LoggerFactory.getLogger(TransactionalEmailService.class);

    private final EmailMessageRepository emailMessageRepository;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final ContactRepository contactRepository;
    private final SuppressionEntryRepository suppressionEntryRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final Outbox outbox;
    private final ObjectMapper objectMapper;
    private final TemplateRenderer templateRenderer;

    public TransactionalEmailService(EmailMessageRepository emailMessageRepository,
                                     TemplateRepository templateRepository,
                                     TemplateVersionRepository templateVersionRepository,
                                     ContactRepository contactRepository,
                                     SuppressionEntryRepository suppressionEntryRepository,
                                     IdempotencyRecordRepository idempotencyRecordRepository,
                                     Outbox outbox,
                                     ObjectMapper objectMapper,
                                     TemplateRenderer templateRenderer) {
        this.emailMessageRepository = emailMessageRepository;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.contactRepository = contactRepository;
        this.suppressionEntryRepository = suppressionEntryRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.templateRenderer = templateRenderer;
    }

    @Transactional
    public QueuedEmail queue(Organization organization, SendEmailRequest request,
                               String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        // Idempotency: replay the original response for the same key + request
        IdempotencyRecord existing = idempotencyRecordRepository
                .findByOrganizationIdAndIdemKey(organization.getId(), idempotencyKey)
                .orElse(null);
        if (existing != null) {
            String requestHash = hashRequest(request);
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new ConflictException(
                        "Idempotency key reused with different request");
            }
            return deserialize(existing.getResponseBody());
        }

        Template template = templateRepository
                .findByIdAndOrganizationId(request.templateId(), organization.getId())
                .orElseThrow(() -> new NotFoundException("Template not found"));
        TemplateVersion version = templateVersionRepository
                .findByTemplateIdAndVersion(template.getId(), template.getActiveVersion())
                .orElseThrow(() -> new IllegalStateException(
                        "Template active version missing"));

        Contact contact = contactRepository
                .findByOrganizationIdAndEmail(organization.getId(), request.to())
                .orElse(null);

        if (contact != null) {
            if (suppressionEntryRepository
                    .existsByOrganizationIdAndEmail(organization.getId(),
                            request.to())) {
                throw new IllegalStateException(
                        "Recipient is suppressed (unsubscribed or bounced)");
            }
            if (contact.getConsentStatus() != com.mailscale.contact.ConsentStatus.GRANTED) {
                throw new IllegalStateException("Recipient has not granted consent");
            }
        }

        Map<String, Object> variables = request.variables() != null
                ? request.variables() : Map.of();
        String subject = templateRenderer.render(version.getSubject(), variables);
        String htmlBody = templateRenderer.render(version.getHtmlBody(), variables);
        String textBody = templateRenderer.render(version.getTextBody(), variables);

        UUID messageId = EmailMessage.deriveId(idempotencyKey, organization.getSlug());
        String correlationId = UUID.randomUUID().toString();

        EmailMessage message = new EmailMessage(
                messageId, organization, MessageType.TRANSACTIONAL,
                request.to(), request.toName(), subject, htmlBody, textBody,
                template, contact, null, correlationId, idempotencyKey);
        emailMessageRepository.save(message);

        String payload = serializePayload(messageId, organization, request,
                subject, htmlBody, textBody);
        outbox.record("EMAIL_MESSAGE", messageId.toString(),
                "EMAIL_QUEUED", payload);

        QueuedEmail response = new QueuedEmail(messageId.toString(),
                correlationId, "QUEUED");

        idempotencyRecordRepository.save(new IdempotencyRecord(organization,
                idempotencyKey, hashRequest(request), 202,
                serializeResponse(response),
                Instant.now().plus(java.time.Duration.ofHours(24))));

        log.info("Transactional email queued id={} org={} template={}",
                messageId, organization.getSlug(), template.getId());
        return response;
    }

    @Transactional(readOnly = true)
    public EmailMessage status(Organization organization, UUID messageId) {
        return emailMessageRepository.findById(messageId)
                .filter(m -> m.getOrganization().getId().equals(organization.getId()))
                .orElseThrow(() -> new NotFoundException("Message not found"));
    }

    public EmailMessage statusByCorrelationId(Organization organization,
                                                  String correlationId) {
        return emailMessageRepository.findByCorrelationId(correlationId)
                .filter(m -> m.getOrganization().getId().equals(organization.getId()))
                .orElseThrow(() -> new NotFoundException("Message not found"));
    }

    private String serializePayload(UUID messageId, Organization organization,
                                    SendEmailRequest request, String subject,
                                    String htmlBody, String textBody) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "messageId", messageId.toString(),
                    "organizationId", organization.getId(),
                    "toEmail", request.to(),
                    "toName", request.toName(),
                    "subject", subject,
                    "htmlBody", htmlBody,
                    "textBody", textBody,
                    "templateId", request.templateId(),
                    "idempotencyKey", request.idempotencyKey()
            ));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize outbox payload", e);
        }
    }

    private String serializeResponse(QueuedEmail response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize response", e);
        }
    }

    private QueuedEmail deserialize(String body) {
        try {
            return objectMapper.readValue(body, QueuedEmail.class);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize response", e);
        }
    }

    private String hashRequest(SendEmailRequest request) {
        try {
            String raw = request.to() + "|" + request.templateId() + "|"
                    + request.variables();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record SendEmailRequest(String to, String toName, Long templateId,
                                      Map<String, Object> variables,
                                      String idempotencyKey) {
    }

    public record QueuedEmail(String messageId, String correlationId,
                                  String status) {
    }
}
