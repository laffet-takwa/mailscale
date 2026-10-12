package com.mailscale.ai;

import java.util.List;

/**
 * Structured output of the AI assistant.
 *
 * <p>Callers must validate this content with the campaign
 * validation rules before use; the generator never bypasses
 * policy checks.
 */
public record GeneratedContent(
        String subject,
        String htmlBody,
        String textBody,
        List<String> callsToAction,
        List<Variant> variants,
        String explanation,
        String provider,
        long latencyMs) {

    public record Variant(String subject, String previewText) {
    }
}
