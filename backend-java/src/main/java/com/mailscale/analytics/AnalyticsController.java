package com.mailscale.analytics;

import com.mailscale.analytics.AnalyticsService.CampaignSummary;
import com.mailscale.analytics.AnalyticsService.DashboardStats;
import com.mailscale.common.OrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard metrics")
    public ResponseEntity<DashboardStats> dashboard() {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(analyticsService.dashboard(org));
    }

    @GetMapping("/campaigns")
    @Operation(summary = "Campaign summaries")
    public ResponseEntity<List<CampaignSummary>> campaignSummaries() {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(
                analyticsService.campaignSummaries(org));
    }
}
