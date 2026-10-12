package com.mailscale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mailscale.auth.Organization;
import com.mailscale.auth.OrganizationRepository;
import com.mailscale.email.EmailMessage;
import com.mailscale.email.EmailMessageRepository;
import com.mailscale.email.IdempotencyRecordRepository;
import com.mailscale.email.MessageStatus;
import com.mailscale.email.TransactionalEmailService;
import com.mailscale.email.TransactionalEmailService.QueuedEmail;
import com.mailscale.email.TransactionalEmailService.SendEmailRequest;
import com.mailscale.outbox.OutboxEventRepository;
import com.mailscale.suppression.SuppressionEntry;
import com.mailscale.suppression.SuppressionEntryRepository;
import com.mailscale.suppression.SuppressionReason;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionalEmailIntegrationTest {

    @Autowired
    private TransactionalEmailService emailService;

    @Autowired
    private EmailMessageRepository emailMessageRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SuppressionEntryRepository suppressionEntryRepository;

    private Organization org;

    @BeforeEach
    void setUp() {
        org = organizationRepository.findBySlug("demo")
                .orElseThrow();
    }

    @Test
    void queueCreatesMessageAndOutboxEvent() {
        QueuedEmail response = emailService.queue(org,
                new SendEmailRequest("anna@example.com", "Anna",
                        1L, Map.of("firstName", "Anna"),
                        "idem-1"),
                "idem-1");

        assertEquals("QUEUED", response.status());
        UUID messageId = UUID.fromString(response.messageId());

        EmailMessage message = emailMessageRepository
                .findById(messageId).orElseThrow();
        assertEquals(MessageStatus.QUEUED, message.getStatus());
        assertEquals("anna@example.com", message.getToEmail());
        assertTrue(message.getSubject().contains("Anna"));

        assertTrue(outboxEventRepository.findUnpublished()
                .stream().anyMatch(
                        e -> e.getEventType().equals("EMAIL_QUEUED")));

        assertTrue(idempotencyRecordRepository
                .findByOrganizationIdAndIdemKey(org.getId(),
                        "idem-1").isPresent());
    }

    @Test
    void sameIdempotencyKeyReturnsSameResponse() {
        SendEmailRequest request = new SendEmailRequest(
                "anna@example.com", "Anna", 1L,
                Map.of("firstName", "Anna"), "idem-2");
        QueuedEmail first = emailService.queue(org, request,
                "idem-2");
        QueuedEmail second = emailService.queue(org, request,
                "idem-2");
        assertEquals(first.messageId(), second.messageId());
        assertEquals(first.correlationId(),
                second.correlationId());
    }

    @Test
    void idempotencyKeyReuseWithDifferentRequestIsRejected() {
        emailService.queue(org,
                new SendEmailRequest("anna@example.com", "Anna",
                        1L, Map.of("firstName", "Anna"),
                        "idem-3"),
                "idem-3");
        assertThrows(com.mailscale.common.ConflictException.class,
                () -> emailService.queue(org,
                        new SendEmailRequest("ben@example.com",
                                "Ben", 1L, Map.of(),
                                "idem-3"),
                        "idem-3"));
    }

    @Test
    void missingIdempotencyKeyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> emailService.queue(org,
                        new SendEmailRequest("anna@example.com",
                                "Anna", 1L, Map.of(), null),
                        null));
    }

    @Test
    void suppressedRecipientIsRejected() {
        suppressionEntryRepository.save(
                new SuppressionEntry(org, "anna@example.com",
                        SuppressionReason.MANUAL));
        assertThrows(IllegalStateException.class,
                () -> emailService.queue(org,
                        new SendEmailRequest("anna@example.com",
                                "Anna", 1L, Map.of(),
                                "idem-4"),
                        "idem-4"));
    }
}
