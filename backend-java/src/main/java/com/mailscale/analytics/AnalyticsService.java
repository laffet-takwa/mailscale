package com.mailscale.analytics;

import com.mailscale.auth.Organization;
import com.mailscale.campaign.CampaignRecipientRepository;
import com.mailscale.campaign.CampaignRepository;
import com.mailscale.campaign.RecipientStatus;
import com.mailscale.contact.ContactRepository;
import com.mailscale.email.EmailMessageRepository;
import com.mailscale.email.MessageStatus;
import com.mailscale.suppression.SuppressionEntryRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard analytics computed from real database state.
 *
 * <p>All counts use indexed queries on (organization_id, ...)
 * columns; per-day series use range scans on the
 * (organization_id, created_at) index, so the dashboard cost is
 * O(days x log n) rather than O(n).
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(
            AnalyticsService.class);

    private final EmailMessageRepository emailMessageRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignRecipientRepository campaignRecipientRepository;
    private final ContactRepository contactRepository;
    private final SuppressionEntryRepository suppressionEntryRepository;

    public AnalyticsService(EmailMessageRepository emailMessageRepository,
                              CampaignRepository campaignRepository,
                              CampaignRecipientRepository campaignRecipientRepository,
                              ContactRepository contactRepository,
                              SuppressionEntryRepository suppressionEntryRepository) {
        this.emailMessageRepository = emailMessageRepository;
        this.campaignRepository = campaignRepository;
        this.campaignRecipientRepository = campaignRecipientRepository;
        this.contactRepository = contactRepository;
        this.suppressionEntryRepository = suppressionEntryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStats dashboard(Organization org) {
        Instant dayStart = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        long totalCampaigns = campaignRepository
                .findByOrganizationIdOrderByCreatedAtDesc(org.getId()).size();
        long queued = emailMessageRepository
                .countByOrganizationIdAndStatusInAndCreatedAtAfter(org.getId(),
                        List.of(MessageStatus.QUEUED,
                                MessageStatus.PROCESSING), dayStart);
        long sent = emailMessageRepository
                .countByOrganizationIdAndStatusInAndCreatedAtAfter(org.getId(),
                        List.of(MessageStatus.SENT,
                                MessageStatus.DELIVERED), dayStart);
        long bounced = emailMessageRepository
                .countByOrganizationIdAndStatusInAndCreatedAtAfter(org.getId(),
                        List.of(MessageStatus.BOUNCED), dayStart);
        long failed = emailMessageRepository
                .countByOrganizationIdAndStatusInAndCreatedAtAfter(org.getId(),
                        List.of(MessageStatus.FAILED), dayStart);
        long suppressed = suppressionEntryRepository
                .findByOrganizationId(org.getId()).size();
        long activeContacts = contactRepository
                .countActiveByOrganizationId(org.getId());

        long processed = sent + bounced + failed;
        double deliveryRate = processed == 0 ? 0.0
                : (double) sent / processed * 100.0;

        return new DashboardStats(totalCampaigns, queued, sent, bounced,
                failed, suppressed, activeContacts, deliveryRate,
                perDaySeries(org, 7));
    }

    @Transactional(readOnly = true)
    public List<CampaignSummary> campaignSummaries(Organization org) {
        return campaignRepository
                .findByOrganizationIdOrderByCreatedAtDesc(org.getId())
                .stream()
                .map(c -> {
                    List<?> grouped = campaignRecipientRepository
                            .countByStatusGrouped(c.getId());
                    Map<String, Long> byStatus = new LinkedHashMap<>();
                    for (Object row : grouped) {
                        Map<?, ?> m = (Map<?, ?>) row;
                        byStatus.put(String.valueOf(m.get("status")),
                                ((Number) m.get("count")).longValue());
                    }
                    return new CampaignSummary(c.getId(), c.getName(),
                            c.getStatus().name(), c.getRecipientsCount(),
                            byStatus);
                })
                .toList();
    }

    private List<DailyPoint> perDaySeries(Organization org, int days) {
        List<DailyPoint> series = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minus(i, ChronoUnit.DAYS);
            Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = date.plusDays(1)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            long count = emailMessageRepository
                    .countByOrganizationIdAndCreatedAtBetween(org.getId(),
                            start, end);
            series.add(new DailyPoint(date.toString(), count));
        }
        return series;
    }

    public record DashboardStats(long totalCampaigns, long queued,
                                     long sent, long bounced, long failed,
                                     long suppressed, long activeContacts,
                                     double deliveryRate,
                                     List<DailyPoint> sentPerDay) {
    }

    public record DailyPoint(String date, long sent) {
    }

    public record CampaignSummary(Long id, String name, String status,
                                      int recipientsCount,
                                      Map<String, Long> recipientsByStatus) {
    }
}
