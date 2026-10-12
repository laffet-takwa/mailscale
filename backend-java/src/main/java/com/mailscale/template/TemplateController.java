package com.mailscale.template;

import com.mailscale.common.OrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @Operation(summary = "List templates")
    public ResponseEntity<List<TemplateDto>> list() {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(templateService.list(org));
    }

    @PostMapping
    @Operation(summary = "Create a template with version 1")
    public ResponseEntity<TemplateDto> create(
            @Valid @RequestBody CreateTemplateRequest request) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(templateService.create(org, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template with all versions")
    public ResponseEntity<TemplateDetailDto> get(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(templateService.get(org, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Create a new template version")
    public ResponseEntity<TemplateDto> addVersion(@PathVariable Long id,
                                                    @Valid @RequestBody CreateTemplateRequest request) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(templateService.addVersion(org, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a template")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        templateService.delete(org, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/preview")
    @Operation(summary = "Preview a template with sample data")
    public ResponseEntity<PreviewDto> preview(@PathVariable Long id,
                                                @RequestBody Map<String, Object> variables) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(templateService.preview(org, id, variables));
    }

    public record CreateTemplateRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 500) String subject,
            @NotBlank String htmlBody,
            @NotBlank String textBody) {
    }

    public record TemplateDto(Long id, String name, int activeVersion,
                                  java.time.Instant createdAt) {
    }

    public record TemplateDetailDto(Long id, String name, int activeVersion,
                                        java.time.Instant createdAt,
                                        List<TemplateVersion> versions) {
    }

    public record PreviewDto(String subject, String htmlBody, String textBody) {
    }
}
