package com.mailscale.contact;

import com.mailscale.common.OrganizationContext;
import com.mailscale.contact.ContactService.CreateContactRequest;
import com.mailscale.contact.ContactService.ImportResult;
import com.mailscale.contact.ContactService.UpdateContactRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contacts")
@Tag(name = "Contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    @Operation(summary = "List contacts (paginated, filterable)")
    public ResponseEntity<Page<Contact>> list(
            @PageableDefault(size = 50) Pageable pageable,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ContactStatus status) {
        var org = OrganizationContext.requireOrganization();
        if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(contactService.search(org, q, pageable));
        }
        if (status != null) {
            return ResponseEntity.ok(
                    contactService.findByStatus(org, status, pageable));
        }
        return ResponseEntity.ok(contactService.list(org, pageable));
    }

    @PostMapping
    @Operation(summary = "Create a contact")
    public ResponseEntity<Contact> create(
            @Valid @RequestBody CreateContactRequest request) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(contactService.create(org, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a contact")
    public ResponseEntity<Contact> get(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(contactService.get(org, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a contact")
    public ResponseEntity<Contact> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdateContactRequest request) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(contactService.update(org, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a contact")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        contactService.delete(org, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/unsubscribe")
    @Operation(summary = "Unsubscribe a contact (adds to suppression list)")
    public ResponseEntity<Contact> unsubscribe(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(
                contactService.unsubscribe(org, id, Instant.now()));
    }

    @PatchMapping("/{id}/resubscribe")
    @Operation(summary = "Resubscribe a contact (removes from suppression)")
    public ResponseEntity<Contact> resubscribe(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(
                contactService.resubscribe(org, id, Instant.now()));
    }

    @PostMapping("/import")
    @Operation(summary = "Import contacts from CSV")
    public ResponseEntity<ImportResult> importCsv(
            @RequestBody CsvImportRequest request) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(contactService.importCsv(org, request.csv()));
    }

    public record CsvImportRequest(@NotBlank String csv) {
    }
}
