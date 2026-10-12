package com.mailscale.ai;

import com.mailscale.auth.Organization;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Generates email content with structured output.
 *
 * <p>Implementations must never override consent, suppression,
 * authorization, or rate-limit policies (ADR-0007). All generated
 * content requires human review before a campaign is scheduled.
 */
public interface AiContentGenerator {

    /**
     * Generates campaign content for a brief.
     *
     * @param org the tenant making the request (audited, never
     *            used to bypass policy)
     * @param brief the user's content brief
     * @return structured generated content
     */
    GeneratedContent generate(Organization org, GenerationRequest brief);
}
