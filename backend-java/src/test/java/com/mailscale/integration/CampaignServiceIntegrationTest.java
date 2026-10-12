package com.mailscale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mailscale.auth.Organization;
import com.mailscale.auth.OrganizationRepository;
import com.mailscale.campaign.Campaign;
import com.mailscale.campaign.CampaignRepository;
import com.mailscale.campaign.CampaignService;
import com.mailscale.campaign.CampaignService.CreateCampaignRequest;
import com.mailscale.template.Template;
import com.mailscale.template.TemplateRepository;
import com.mailscale.template.TemplateController.CreateTemplateRequest;
import com.mailscale.template.TemplateController.PreviewDto;
import com.mailscale.template.TemplateController.TemplateDetailDto;
import com.mailscale.template.TemplateController.TemplateDto;
import com.mailscale.template.TemplateService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CampaignServiceIntegrationTest {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private TemplateService templateService;

    private Organization org;
    private Template template;

    @BeforeEach
    void setUp() {
        org = organizationRepository.findBySlug("demo")
                .orElseThrow();
        template = templateRepository
                .findByOrganizationIdAndName(org.getId(),
                        "Welcome Email")
                .orElseThrow();
    }

    @Test
    void createCampaignDraft() {
        Campaign campaign = campaignService.create(org,
                new CreateCampaignRequest("Q1 Launch",
                        1L, template.getId()));
        assertNotNull(campaign.getId());
        assertEquals("DRAFT", campaign.getStatus().name());
    }

    @Test
    void validateFindsNoProblemsForValidCampaign() {
        Campaign campaign = campaignService.create(org,
                new CreateCampaignRequest("Valid",
                        1L, template.getId()));
        List<String> problems = campaignService.validate(org,
                campaign.getId());
        assertTrue(problems.isEmpty());
    }

    @Test
    void validateRejectsFinishedCampaign() {
        Campaign campaign = campaignService.create(org,
                new CreateCampaignRequest("Finished",
                        1L, template.getId()));
        campaignService.schedule(org, campaign.getId(),
                Instant.now().plusSeconds(60));
        campaignService.dispatch(org, campaign.getId());
        List<String> problems = campaignService.validate(org,
                campaign.getId());
        assertTrue(problems.stream().anyMatch(
                p -> p.contains("finished")));
    }

    @Test
    void scheduleAndDispatchCompletesCampaign() {
        Campaign campaign = campaignService.create(org,
                new CreateCampaignRequest("Dispatch",
                        1L, template.getId()));
        Campaign scheduled = campaignService.schedule(org,
                campaign.getId(),
                Instant.now().plusSeconds(60));
        assertEquals("SCHEDULED", scheduled.getStatus().name());

        Campaign dispatched = campaignService.dispatch(org,
                campaign.getId());
        assertEquals("COMPLETED", dispatched.getStatus().name());
    }

    @Test
    void dispatchRejectsUnscheduledCampaign() {
        Campaign campaign = campaignService.create(org,
                new CreateCampaignRequest("NoSchedule",
                        1L, template.getId()));
        assertThrows(IllegalStateException.class,
                () -> campaignService.dispatch(org,
                        campaign.getId()));
    }

    @Test
    void templateVersioning() {
        TemplateDto created = templateService.create(org,
                new CreateTemplateRequest("Versioned",
                        "Subject v1", "<p>v1</p>", "v1"));
        assertEquals(1, created.activeVersion());

        TemplateDto updated = templateService.addVersion(org,
                created.id(),
                new CreateTemplateRequest("Versioned",
                        "Subject v2", "<p>v2</p>", "v2"));
        assertEquals(2, updated.activeVersion());

        TemplateDetailDto detail = templateService.get(org,
                created.id());
        assertEquals(2, detail.versions().size());
    }

    @Test
    void templatePreviewRendersVariables() {
        PreviewDto preview = templateService.preview(org,
                template.getId(),
                java.util.Map.of("firstName", "Anna",
                        "company", "Acme"));
        assertTrue(preview.subject().contains("Anna"));
        assertTrue(preview.htmlBody().contains("Acme"));
    }
}
