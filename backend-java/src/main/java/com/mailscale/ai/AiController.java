package com.mailscale.ai;

import com.mailscale.ai.AiContentGenerator;
import com.mailscale.ai.GenerationRequest;
import com.mailscale.ai.GeneratedContent;
import com.mailscale.common.OrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI assistant endpoint.
 *
 * <p>The AI drafts content only. It never bypasses
 * authorization, consent, suppression, or approval
 * policies (ADR-0007). Generated content must be
 * reviewed before a campaign is scheduled.
 */
@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Assistant")
public class AiController {

    private final AiContentGenerator generator;

    public AiController(AiContentGenerator generator) {
        this.generator = generator;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate email content (subject, body, CTAs)")
    public ResponseEntity<GeneratedContent> generate(
            @Valid @RequestBody GenerationRequest request) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(generator.generate(org, request));
    }

    @PostMapping("/suggest-segments")
    @Operation(summary = "Suggest audience segments from permitted contact attributes")
    public ResponseEntity<java.util.List<String>> suggestSegments(
            @RequestBody SegmentSuggestionRequest request) {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(java.util.List.of(
                "Active contacts with consent",
                "Contacts engaged in the last 30 days",
                "Contacts tagged: " + request.tagHint()));
    }

    @PostMapping("/explain-rejection")
    @Operation(summary = "Explain why a campaign might be rejected by validation")
    public ResponseEntity<String> explainRejection(
            @RequestBody ExplanationRequest request) {
        return ResponseEntity.ok(
                "Common rejection reasons: missing or inactive segment; "
                        + "no active contacts with granted consent; "
                        + "blank template subject; audience over the "
                        + "configured limit; campaign already finished. "
                        + "Your note: " + request.note());
    }

    public record SegmentSuggestionRequest(String tagHint) {
    }

    public record ExplanationRequest(String note) {
    }
}
