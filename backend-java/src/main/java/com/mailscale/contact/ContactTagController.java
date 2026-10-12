package com.mailscale.contact;

import com.mailscale.auth.Organization;
import com.mailscale.common.OrganizationContext;
import com.mailscale.common.ConflictException;
import com.mailscale.common.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
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
@RequestMapping("/api/v1/contact-tags")
@Tag(name = "Contact Tags")
public class ContactTagController {

    private final ContactTagService contactTagService;

    public ContactTagController(ContactTagService contactTagService) {
        this.contactTagService = contactTagService;
    }

    @GetMapping
    @Operation(summary = "List tags")
    public ResponseEntity<List<ContactTag>> list() {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(contactTagService.list(org));
    }

    @PostMapping
    @Operation(summary = "Create a tag")
    public ResponseEntity<ContactTag> create(
            @Valid @RequestBody CreateTagRequest request) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(contactTagService.create(org, request.name()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tag")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        contactTagService.delete(org, id);
        return ResponseEntity.noContent().build();
    }

    public record CreateTagRequest(
            @NotBlank @Size(max = 120) String name) {
    }
}
