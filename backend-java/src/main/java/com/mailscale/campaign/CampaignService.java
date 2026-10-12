package com.mailscale.campaign;

import com.mailscale.auth.Organization;
import com.mailscale.contact.Contact;
import com.mailscale.contact.ContactRepository;
import com.mailscale.contact.Segment;
import com.mailscale.contact.SegmentRepository;
import com.mailscale.common.ConflictException;
import com.mailscale.common.NotFoundException;
import com.mailscale.email.EmailMessage;
import com.mailscale.email.EmailMessageRepository;
import com.mailscale.email.MessageType;
import com.mailscale.outbox.Outbox;
import com.mailscale.suppression.SuppressionEntryRepository;
import com.mailscale.template.Template;
import com.mailscale.template.TemplateRepository;
import com.mailscale.template.TemplateService;
import com.mailscale.template.TemplateVersion;
import com.mailscale.template.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Campaign lifecycle: draft -> scheduled -> processing -> completed,
 * with validation, consent/suppression checks, and batched dispatch.
 *
 * <p>Dispatch processes recipients in pages of {@code DISPATCH_BATCH_SIZE}
 * to bound memory for large recipient lists. Each recipient produces one
 * email message plus one outbox event within a transaction per batch,
 * so partial progress survives a crash (batches are idempotent via the
 * campaign_recipients unique key and status checks).
 */
@Service
public class CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);

    static final int DISPATCH_BATCH_SIZE = 500;
    static final int MAX_AUDIENCE_SIZE = 100_000;
    private static final Pattern VARIABLE_PATTERN =
            Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*}}");

    private final CampaignRepository campaignRepository;
    private final CampaignRecipientRepository campaignRecipientRepository;
    private final SegmentRepository segmentRepository;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final ContactRepository contactRepository;
    private final SuppressionEntryRepository suppressionEntryRepository;
    private final EmailMessageRepository emailMessageRepository;
    private final Outbox outbox;
    private final ObjectMapper objectMapper;
    private final TemplateService templateService;

    public CampaignService(CampaignRepository campaignRepository,
                             CampaignRecipientRepository campaignRecipientRepository,
                             SegmentRepository segmentRepository,
                             TemplateRepository templateRepository,
                             TemplateVersionRepository templateVersionRepository,
                             ContactRepository contactRepository,
                             SuppressionEntryRepository suppressionEntryRepository,
                             EmailMessageRepository emailMessageRepository,
                             Outbox outbox,
                             ObjectMapper objectMapper,
                             TemplateService templateService) {
        this.campaignRepository = campaignRepository;
        this.campaignRecipientRepository = campaignRecipientRepository;
        this.segmentRepository = segmentRepository;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.contactRepository = contactRepository;
        this.suppressionEntryRepository = suppressionEntryRepository;
        this.emailMessageRepository = emailMessageRepository;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.templateService = templateService;
    }

    @Transactional
    public Campaign create(Organization org, CreateCampaignRequest request) {
        Segment segment = segmentRepository
                .findByIdAndOrganizationId(request.segmentId(), org.getId())
                .orElseThrow(() -> new NotFoundException("Segment not found"));
        Template template = templateRepository
                .findByIdAndOrganizationId(request.templateId(), org.getId())
                .orElseThrow(() -> new NotFoundException("Template not found"));
        Campaign campaign = new Campaign(org, request.name(), segment, template);
        campaignRepository.save(campaign);
        return campaign;
    }

    @Transactional(readOnly = true)
    public List<Campaign> list(Organization org) {
        return campaignRepository.findByOrganizationIdOrderByCreatedAtDesc(
                org.getId());
    }

    @Transactional(readOnly = true)
    public Campaign get(Organization org, Long id) {
        return campaignRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
    }

    @Transactional(readOnly = true)
    public List<CampaignRecipient> recipients(Organization org, Long id) {
        Campaign campaign = get(org, id);
        return campaignRecipientRepository.findByCampaignId(campaign.getId());
    }

    /**
     * Validates a campaign against business rules.
     * Returns a list of human-readable problems (empty when valid).
     */
    @Transactional(readOnly = true)
    public List<String> validate(Organization org, Long id) {
        Campaign campaign = get(org, id);
        List<String> problems = new ArrayList<>();
        if (campaign.getTemplate() == null) {
            problems.add("Campaign has no template");
        }
        if (campaign.getSegment() == null) {
            problems.add("Campaign has no recipient segment");
        }
        if (campaign.getStatus() == CampaignStatus.PROCESSING) {
            problems.add("Campaign is already processing");
        }
        if (campaign.getStatus() == CampaignStatus.COMPLETED
                || campaign.getStatus() == CampaignStatus.CANCELLED
                || campaign.getStatus() == CampaignStatus.FAILED) {
            problems.add("Campaign is already finished: " + campaign.getStatus());
        }
        if (campaign.getSegment() != null) {
            long audience = contactRepository
                    .countActiveByOrganizationId(org.getId());
            if (audience == 0) {
                problems.add("Segment has no active contacts");
            }
            if (audience > MAX_AUDIENCE_SIZE) {
                problems.add("Audience of " + audience
                        + " exceeds the limit of " + MAX_AUDIENCE_SIZE);
            }
        }
        if (campaign.getTemplate() != null) {
            TemplateVersion version = templateVersionRepository
                    .findByTemplateIdAndVersion(
                            campaign.getTemplate().getId(),
                            campaign.getTemplate().getActiveVersion())
                    .orElse(null);
            if (version == null) {
                problems.add("Template has no active version");
            } else if (version.getSubject().isBlank()) {
                problems.add("Template subject is blank");
            }
        }
        return problems;
    }

    @Transactional
    public Campaign schedule(Organization org, Long id, Instant scheduledAt) {
        Campaign campaign = get(org, id);
        List<String> problems = validate(org, id);
        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Campaign validation failed: " + String.join("; ", problems));
        }
        campaign.schedule(scheduledAt);
        return campaignRepository.save(campaign);
    }

    /**
     * Dispatches a scheduled campaign: evaluates the segment,
     * checks consent and suppression, and queues one email per
     * recipient in bounded batches.
     */
    @Transactional
    public Campaign dispatch(Organization org, Long id) {
        Campaign campaign = get(org, id);
        if (campaign.getStatus() != CampaignStatus.SCHEDULED
                && campaign.getStatus() != CampaignStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Campaign is not dispatchable: " + campaign.getStatus());
        }

        TemplateVersion version = templateVersionRepository
                .findByTemplateIdAndVersion(campaign.getTemplate().getId(),
                        campaign.getTemplate().getActiveVersion())
                .orElseThrow(() -> new IllegalStateException(
                        "Template active version missing"));

        if (campaign.getStatus() == CampaignStatus.SCHEDULED) {
            long audience = contactRepository
                    .countActiveByOrganizationId(org.getId());
            if (audience > MAX_AUDIENCE_SIZE) {
                campaign.fail("Audience exceeds limit");
                campaignRepository.save(campaign);
                throw new IllegalStateException(
                        "Audience exceeds limit of " + MAX_AUDIENCE_SIZE);
            }
            campaign.startProcessing(Instant.now(), (int) audience);
            campaignRepository.save(campaign);
        }

        int page = 0;
        int queued = 0;
        int suppressed = 0;
        int skipped = 0;
        List<Contact> batch;
        do {
            batch = contactRepository.findDeliverable(org.getId(),
                            PageRequest.of(page++, DISPATCH_BATCH_SIZE))
                    .getContent();
            for (Contact contact : batch) {
                if (suppressionEntryRepository
                        .existsByOrganizationIdAndEmail(org.getId(),
                                contact.getEmail())) {
                    suppressed++;
                    markRecipient(campaign, contact,
                            RecipientStatus.SUPPRESSED);
                    continue;
                }
                if (campaignRecipientRepository
                        .findByCampaignIdAndContactId(campaign.getId(),
                                contact.getId())
                        .map(cr -> cr.getStatus() != RecipientStatus.PENDING)
                        .orElse(false)) {
                    continue; // already processed in a previous attempt
                }
                Map<String, Object> variables = Map.of(
                        "firstName",
                        contact.getFirstName() != null
                                ? contact.getFirstName() : "",
                        "lastName",
                        contact.getLastName() != null
                                ? contact.getLastName() : "",
                        "email", contact.getEmail());
                String subject = templateService.render(
                        version.getSubject(), variables);
                String htmlBody = templateService.render(
                        version.getHtmlBody(), variables);
                String textBody = templateService.render(
                        version.getTextBody(), variables);

                UUID messageId = UUID.randomUUID();
                EmailMessage message = new EmailMessage(messageId, org,
                        MessageType.CAMPAIGN, contact.getEmail(),
                        contact.getFirstName(), subject, htmlBody, textBody,
                        campaign.getTemplate(), contact, campaign,
                        UUID.randomUUID().toString(), null);
                emailMessageRepository.save(message);

                String payload = serializePayload(messageId, org, campaign,
                        contact, subject, htmlBody, textBody);
                outbox.record("EMAIL_MESSAGE", messageId.toString(),
                        "CAMPAIGN_EMAIL_QUEUED", payload);

                markRecipient(campaign, contact, RecipientStatus.QUEUED);
                queued++;
            }
        } while (batch.size() == DISPATCH_BATCH_SIZE);

        campaign.complete(Instant.now());
        campaignRepository.save(campaign);
        log.info("Campaign {} dispatched: queued={} suppressed={} skipped={}",
                campaign.getId(), queued, suppressed, skipped);
        return campaign;
    }

    @Transactional
    public Campaign cancel(Organization org, Long id) {
        Campaign campaign = get(org, id);
        campaign.cancel(Instant.now());
        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign fail(Organization org, Long id, String reason) {
        Campaign campaign = get(org, id);
        campaign.fail(reason);
        return campaignRepository.save(campaign);
    }

    /**
     * Scheduled dispatch runner: finds due campaigns and dispatches them.
     */
    @Transactional
    public int dispatchDue(Organization org) {
        List<Campaign> due = campaignRepository.findDueForDispatch(org.getId(),
                Instant.now());
        for (Campaign campaign : due) {
            try {
                dispatch(org, campaign.getId());
            } catch (Exception e) {
                log.error("Failed to dispatch campaign {}", campaign.getId(), e);
                campaign.fail(e.getMessage());
                campaignRepository.save(campaign);
            }
        }
        return due.size();
    }

    private void markRecipient(Campaign campaign, Contact contact,
                                 RecipientStatus status) {
        CampaignRecipient recipient = campaignRecipientRepository
                .findByCampaignIdAndContactId(campaign.getId(),
                        contact.getId())
                .orElse(new CampaignRecipient(campaign, contact));
        if (status == RecipientStatus.SUPPRESSED) {
            recipient.markSuppressed();
        } else {
            recipient.markQueued(Instant.now());
        }
        campaignRecipientRepository.save(recipient);
    }

    private String serializePayload(UUID messageId, Organization org,
                                    Campaign campaign, Contact contact,
                                    String subject, String htmlBody,
                                    String textBody) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("messageId", messageId.toString());
            node.put("organizationId", org.getId());
            node.put("toEmail", contact.getEmail());
            node.put("toName", contact.getFirstName());
            node.put("subject", subject);
            node.put("htmlBody", htmlBody);
            node.put("textBody", textBody);
            if (campaign.getTemplate() != null) {
                node.put("templateId", campaign.getTemplate().getId());
            }
            if (campaign.getId() != null) {
                node.put("campaignId", campaign.getId());
            }
            node.put("contactId", contact.getId());
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot serialize outbox payload", e);
        }
    }

    public record CreateCampaignRequest(String name, Long segmentId,
                                          Long templateId) {
    }
}
