package com.mailscale.campaign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled campaign dispatch runner.
 *
 * <p>Runs every 30 seconds and dispatches campaigns whose
 * scheduled time has passed, across all organizations.
 */
@Component
public class CampaignDispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(
            CampaignDispatchScheduler.class);

    private final CampaignDispatchCoordinator dispatchCoordinator;

    public CampaignDispatchScheduler(
            CampaignDispatchCoordinator dispatchCoordinator) {
        this.dispatchCoordinator = dispatchCoordinator;
    }

    @Scheduled(fixedDelay = 30_000)
    public void dispatchDueCampaigns() {
        int dispatched = dispatchCoordinator.dispatchDueForAllOrganizations();
        if (dispatched > 0) {
            log.info("Dispatched {} due campaigns", dispatched);
        }
    }
}
