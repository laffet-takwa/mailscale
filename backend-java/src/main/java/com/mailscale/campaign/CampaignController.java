package com.mailscale.campaign;

import com.mailscale.campaign.CampaignService.CreateCampaignRequest;
import com.mailscale.common.OrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/campaigns")
@Tag(name = "Campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    @Operation(summary = "List campaigns")
    public ResponseEntity<List<Campaign>> list() {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(campaignService.list(org));
    }

    @PostMapping
    @Operation(summary = "Create a draft campaign")
    public ResponseEntity<Campaign> create(
            @Valid @RequestBody CreateCampaignRequest request) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(campaignService.create(org, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a campaign")
    public ResponseEntity<Campaign> get(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(campaignService.get(org, id));
    }

    @GetMapping("/{id}/recipients")
    @Operation(summary = "List campaign recipients with status")
    public ResponseEntity<List<CampaignRecipient>> recipients(
            @PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(campaignService.recipients(org, id));
    }

    @GetMapping("/{id}/validate")
    @Operation(summary = "Validate a campaign against business rules")
    public ResponseEntity<Map<String, Object>> validate(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        List<String> problems = campaignService.validate(org, id);
        return ResponseEntity.ok(Map.of(
                "valid", problems.isEmpty(),
                "problems", problems));
    }

    @PostMapping("/{id}/schedule")
    @Operation(summary = "Schedule a campaign for dispatch")
    public ResponseEntity<Campaign> schedule(@PathVariable Long id,
                                                 @RequestBody ScheduleRequest request) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(
                campaignService.schedule(org, id, request.scheduledAt()));
    }

    @PostMapping("/{id}/dispatch")
    @Operation(summary = "Dispatch a campaign immediately")
    public ResponseEntity<Campaign> dispatch(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(campaignService.dispatch(org, id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a scheduled or processing campaign")
    public ResponseEntity<Campaign> cancel(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(campaignService.cancel(org, id));
    }

    public record ScheduleRequest(
            @NotNull Instant scheduledAt) {
    }
}
