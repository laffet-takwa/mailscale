package com.mailscale.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CampaignTest {

    @Test
    void lifecycleTransitions() {
        Campaign campaign = new Campaign(null, "Test", null, null);
        assertEquals(CampaignStatus.DRAFT, campaign.getStatus());

        campaign.schedule(java.time.Instant.now().plusSeconds(60));
        assertEquals(CampaignStatus.SCHEDULED, campaign.getStatus());

        campaign.startProcessing(java.time.Instant.now(), 10);
        assertEquals(CampaignStatus.PROCESSING, campaign.getStatus());

        campaign.complete(java.time.Instant.now());
        assertEquals(CampaignStatus.COMPLETED, campaign.getStatus());
    }

    @Test
    void cancelFromScheduledOrProcessing() {
        Campaign campaign = new Campaign(null, "Test", null, null);
        campaign.schedule(java.time.Instant.now().plusSeconds(60));
        campaign.cancel(java.time.Instant.now());
        assertEquals(CampaignStatus.CANCELLED, campaign.getStatus());
    }

    @Test
    void cancelFromDraftThrows() {
        Campaign campaign = new Campaign(null, "Test", null, null);
        assertThrows(IllegalStateException.class,
                () -> campaign.cancel(java.time.Instant.now()));
    }

    @Test
    void failFromProcessing() {
        Campaign campaign = new Campaign(null, "Test", null, null);
        campaign.schedule(java.time.Instant.now().plusSeconds(60));
        campaign.startProcessing(java.time.Instant.now(), 5);
        campaign.fail("boom");
        assertEquals(CampaignStatus.FAILED, campaign.getStatus());
        assertTrue(campaign.getFailureReason().contains("boom"));
    }

    @Test
    void scheduleFromProcessingThrows() {
        Campaign campaign = new Campaign(null, "Test", null, null);
        campaign.schedule(java.time.Instant.now().plusSeconds(60));
        campaign.startProcessing(java.time.Instant.now(), 1);
        assertThrows(IllegalStateException.class,
                () -> campaign.schedule(java.time.Instant.now()));
    }
}
