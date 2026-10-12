package com.mailscale.email;

import com.mailscale.common.OrganizationContext;
import com.mailscale.email.TransactionalEmailService.QueuedEmail;
import com.mailscale.email.TransactionalEmailService.SendEmailRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emails")
@Tag(name = "Transactional Email API")
public class TransactionalEmailController {

    private final TransactionalEmailService emailService;
    private final RateLimitService rateLimitService;

    public TransactionalEmailController(TransactionalEmailService emailService,
                                            RateLimitService rateLimitService) {
        this.emailService = emailService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    @Operation(summary = "Queue a transactional email (idempotent)")
    public ResponseEntity<QueuedEmail> send(
            @Valid @RequestBody SendEmailRequest request) {
        var organization = OrganizationContext.requireOrganization();
        rateLimitService.check("email:" + organization.getId());
        QueuedEmail response = emailService.queue(organization, request,
                request.idempotencyKey());
        return ResponseEntity.accepted().body(response);
    }

    /**
     * Lists email messages for the current organization.
     *
     * <p>Always scoped to the caller's organization; the status
     * parameter is an optional filter for the email-logs view.
     */
    @GetMapping
    @Operation(summary = "List email messages with optional status filter")
    public ResponseEntity<Page<EmailMessage>> list(
            @PageableDefault(size = 50) Pageable pageable,
            @RequestParam(required = false) MessageStatus status,
            @RequestParam(required = false) MessageType messageType) {
        var organization = OrganizationContext.requireOrganization();
        Page<EmailMessage> page = status != null
                ? emailService.findByStatus(
                        organization, status, messageType, pageable)
                : messageType != null
                    ? emailService.list(
                            organization, messageType, pageable)
                    : emailService.listPage(
                            organization, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{messageId}")
    @Operation(summary = "Get message delivery status")
    public ResponseEntity<EmailMessage> status(
            @PathVariable UUID messageId) {
        var organization = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(emailService.status(organization, messageId));
    }

    @GetMapping("/correlation/{correlationId}")
    @Operation(summary = "Get message by correlation ID")
    public ResponseEntity<EmailMessage> statusByCorrelation(
            @PathVariable String correlationId) {
        var organization = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(
                emailService.statusByCorrelationId(organization, correlationId));
    }
}
