package com.mailscale.ai;

import com.mailscale.ai.GenerationRequest.Tone;
import com.mailscale.auth.Organization;
import com.mailscale.common.RateLimitExceededException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

/**
 * OpenAI-compatible generator.
 *
 * <p>Calls any OpenAI-compatible endpoint with a JSON-schema
 * constrained response. Implements:
 * <ul>
 *   <li>Timeout ({@code AI_TIMEOUT_SECONDS})</li>
 *   <li>Bounded retries with exponential backoff + jitter (max 3)</li>
 *   <li>Token cap ({@code AI_MAX_TOKENS})</li>
 *   <li>Cost-aware logging (no prompt content in logs)</li>
 *   <li>Structured output validation</li>
 * </ul>
 *
 * <p>Prompt-injection safeguards: the brief is passed as user content
 * only; the system prompt contains no recipient or organization data.
 */
@Component
@ConditionalOnProperty(name = "mailscale.ai.provider",
        havingValue = "openai-compatible")
public class OpenAiContentGenerator implements AiContentGenerator {

    private static final Logger log = LoggerFactory.getLogger(
            OpenAiContentGenerator.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final int JITTER_BOUND_MS = 400;

    private final WebClient webClient;
    private final String model;
    private final int maxTokens;
    private final Duration timeout;
    private final Random jitter = new Random();

    public OpenAiContentGenerator(
            WebClient.Builder webClientBuilder,
            @Value("${mailscale.ai.api-base-url}") String apiBaseUrl,
            @Value("${mailscale.ai.api-key}") String apiKey,
            @Value("${mailscale.ai.model}") String model,
            @Value("${mailscale.ai.timeout-seconds}") int timeoutSeconds,
            @Value("${mailscale.ai.max-tokens}") int maxTokens) {
        this.webClient = webClientBuilder
                .baseUrl(apiBaseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.model = model;
        this.maxTokens = maxTokens;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public GeneratedContent generate(Organization org,
            GenerationRequest request) {
        long start = System.currentTimeMillis();
        String systemPrompt = buildSystemPrompt(request.tone());
        String userPrompt = buildUserPrompt(request);

        CompletionResult result = null;
        long attemptDelay = 1000;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                result = callApi(systemPrompt, userPrompt);
                break;
            } catch (WebClientResponseException.TooManyRequests e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new RateLimitExceededException(
                            "AI provider rate limit reached");
                }
                attemptDelay = backoffWithJitter(attemptDelay);
                sleep(attemptDelay);
            } catch (WebClientResponseException e) {
                if (e.getStatusCode().is5xxServerError()
                        && attempt < MAX_ATTEMPTS) {
                    attemptDelay = backoffWithJitter(attemptDelay);
                    sleep(attemptDelay);
                } else {
                    throw new IllegalStateException(
                            "AI provider error: " + e.getStatusCode());
                }
            }
        }

        if (result == null || !result.valid()) {
            throw new IllegalStateException(
                    "AI provider returned invalid structured output");
        }

        long latency = System.currentTimeMillis() - start;
        log.info("AI generation org={} model={} latencyMs={} "
                        + "tokensUsed~{}", org.getSlug(), model,
                latency, result.usageTokens());
        return new GeneratedContent(
                result.subject(), result.htmlBody(), result.textBody(),
                result.callsToAction(), result.variants(),
                result.explanation(), "openai-compatible", latency);
    }

    private CompletionResult callApi(String systemPrompt,
            String userPrompt) {
        Map<String, Object> request = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system",
                                "content", systemPrompt),
                        Map.of("role", "user",
                                "content", userPrompt)));
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CompletionResult.class)
                .timeout(timeout)
                .retryWhen(Retry.fixedDelay(1,
                        Duration.ofMillis(200)))
                .block();
    }

    private String buildSystemPrompt(Tone tone) {
        return "You are an email marketing copywriter. Respond ONLY "
                + "with a JSON object having keys: subject, htmlBody, "
                + "textBody, callsToAction (array of strings), "
                + "variants (array of {subject, previewText}), "
                + "explanation. Tone: " + tone;
    }

    private String buildUserPrompt(GenerationRequest request) {
        return "Product: " + request.product()
                + "\nAudience: " + request.audience()
                + "\nBrief: " + request.brief()
                + "\nGenerate " + request.variantCount()
                + " subject variants.";
    }

    private long backoffWithJitter(long base) {
        return base + jitter.nextInt(JITTER_BOUND_MS);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }

    /**
     * Response payload shape (Jackson-mapped). The provider field
     * is absent in the API response, so it is ignored here.
     */
    public record CompletionResult(
            String subject, String htmlBody, String textBody,
            List<String> callsToAction,
            List<GeneratedContent.Variant> variants,
            String explanation,
            Usage usage) {

        boolean valid() {
            return subject != null && htmlBody != null
                    && textBody != null;
        }

        long usageTokens() {
            return usage != null ? usage.totalTokens() : 0;
        }
    }

    public record Usage(int promptTokens, int completionTokens,
                             long totalTokens) {
    }
}
