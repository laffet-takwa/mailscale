package com.mailscale.campaign;

import com.mailscale.auth.Organization;
import com.mailscale.auth.OrganizationRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dispatches due campaigns across all organizations.
 *
 * <p>The scheduler cannot rely on a request-scoped organization
 * context, so it iterates organizations explicitly. Each dispatch
 * runs in its own transaction via the service layer.
 */
@Service
public class CampaignDispatchCoordinator {

    private static final Logger log = LoggerFactory.getLogger(
            CampaignDispatchCoordinator.class);

    private final OrganizationRepository organizationRepository;
    private final CampaignService campaignService;

    public CampaignDispatchCoordinator(
            OrganizationRepository organizationRepository,
            CampaignService campaignService) {
        this.organizationRepository = organizationRepository;
        this.campaignService = campaignService;
    }

    @Transactional
    public int dispatchDueForAllOrganizations() {
        List<Organization> organizations = organizationRepository.findAll();
        int dispatched = 0;
        for (Organization org : organizations) {
            try {
                dispatched += campaignService.dispatchDue(org);
            } catch (Exception e) {
                log.error("Failed to dispatch campaigns for org {}",
                        org.getSlug(), e);
            }
        }
        return dispatched;
    }
}
