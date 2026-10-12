package com.mailscale.contact;

import com.mailscale.auth.Organization;
import com.mailscale.common.OrganizationContext;
import com.mailscale.common.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/segments")
@Tag(name = "Segments")
public class SegmentController {

    private final SegmentService segmentService;

    public SegmentController(SegmentService segmentService) {
        this.segmentService = segmentService;
    }

    @GetMapping
    @Operation(summary = "List segments")
    public ResponseEntity<List<Segment>> list() {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(segmentService.list(org));
    }

    @PostMapping
    @Operation(summary = "Create a segment")
    public ResponseEntity<Segment> create(
            @Valid @RequestBody SegmentService.CreateSegmentRequest request) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(segmentService.create(org, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a segment")
    public ResponseEntity<Segment> get(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(segmentService.get(org, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a segment")
    public ResponseEntity<Segment> update(@PathVariable Long id,
                                            @Valid @RequestBody SegmentService.CreateSegmentRequest request) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(segmentService.update(org, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a segment")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        var org = OrganizationContext.requireOrganization();
        segmentService.delete(org, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/evaluate")
    @Operation(summary = "Evaluate segment membership (paginated)")
    public ResponseEntity<Page<Contact>> evaluate(@PathVariable Long id,
                                                    @PageableDefault(size = 50) Pageable pageable) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(segmentService.evaluate(org, id, pageable));
    }
}
