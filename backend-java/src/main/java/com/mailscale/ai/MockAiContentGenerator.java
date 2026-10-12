package com.mailscale.ai;

import com.mailscale.auth.Organization;
import com.mailscale.ai.GenerationRequest.Tone;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic offline generator used when {@code AI_PROVIDER=mock}.
 *
 * <p>Used for local development, demonstrations, and tests. Output is
 * stable for a given brief so tests can assert on it.
 */
@Component
@ConditionalOnProperty(name = "mailscale.ai.provider",
        havingValue = "mock", matchIfMissing = true)
public class MockAiContentGenerator implements AiContentGenerator {

    private static final Logger log = LoggerFactory.getLogger(
            MockAiContentGenerator.class);

    @Override
    public GeneratedContent generate(Organization org,
            GenerationRequest request) {
        long start = System.currentTimeMillis();
        String product = request.product() != null
                ? request.product() : "our platform";
        String audience = request.audience() != null
                ? request.audience() : "your team";

        String subject = switch (request.tone()) {
            case FRIENDLY -> "Hi there - " + product + " is ready for "
                    + audience;
            case URGENT -> "Action needed: " + product + " update for "
                    + audience;
            case MINIMAL -> product + " update";
            default -> "Introducing " + product + " for " + audience;
        };

        String htmlBody = "<html><body>"
                + "<h1>" + subject + "</h1>"
                + "<p>" + request.brief() + "</p>"
                + "<p>Best regards,<br>The " + product + " team</p>"
                + "</body></html>";
        String textBody = subject + "\n\n" + request.brief()
                + "\n\nBest regards,\nThe " + product + " team";

        List<String> ctas = List.of(
                "Learn more about " + product,
                "Get started today");

        List<GeneratedContent.Variant> variants = new ArrayList<>();
        for (int i = 1; i <= request.variantCount(); i++) {
            variants.add(new GeneratedContent.Variant(
                    subject + " (variant " + i + ")",
                    "Preview text " + i + " for " + audience));
        }

        String explanation = "Mock generation: brief of "
                + request.brief().length() + " chars, tone "
                + request.tone()
                + ". Generated deterministically offline.";

        long latency = System.currentTimeMillis() - start;
        log.info("Mock AI generation org={} latencyMs={}",
                org.getSlug(), latency);
        return new GeneratedContent(subject, htmlBody, textBody, ctas,
                variants, explanation, "mock", latency);
    }
}
