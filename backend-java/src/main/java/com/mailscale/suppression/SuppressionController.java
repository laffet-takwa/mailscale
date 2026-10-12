package com.mailscale.suppression;

import com.mailscale.auth.Organization;
import com.mailscale.common.ConflictException;
import com.mailscale.common.NotFoundException;
import com.mailscale.common.OrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suppressions")
@Tag(name = "Suppression List")
public class SuppressionController {

    private final SuppressionService suppressionService;

    public SuppressionController(SuppressionService suppressionService) {
        this.suppressionService = suppressionService;
    }

    @GetMapping
    @Operation(summary = "List suppression entries")
    public ResponseEntity<List<SuppressionEntry>> list() {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(suppressionService.list(org));
    }

    @PostMapping
    @Operation(summary = "Add an email to the suppression list")
    public ResponseEntity<SuppressionEntry> add(
            @Valid @RequestBody AddSuppressionRequest request) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(suppressionService.add(org, request.email(),
                request.reason()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a suppression entry")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        suppressionService.remove(org, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check")
    @Operation(summary = "Check if an email is suppressed")
    public ResponseEntity<Boolean> check(@NotBlank @Email String email) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(
                suppressionService.isSuppressed(org, email));
    }

    public record AddSuppressionRequest(
            @NotBlank @Email String email,
            @NotNull SuppressionReason reason) {
    }
}
