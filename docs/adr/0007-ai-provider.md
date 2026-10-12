# ADR-0007: AI provider abstraction (OpenAI-compatible adapter)

**Status:** Accepted
**Date:** 2026-10-12

## Context

The AI assistant must generate subject lines, content, CTAs, rewrites, and
audience suggestions with structured output. The project must run offline for
demonstration and tests.

## Decision

- Define a `AiContentGenerator` interface with typed request/response records
  (subject line, HTML body, text body, CTAs, variants, segment suggestions,
  validation explanations).
- Implement:
  - `OpenAiContentGenerator`: WebClient call to any OpenAI-compatible
    endpoint (`AI_API_BASE_URL`), JSON-schema structured output, timeout,
    bounded retries with jitter, token caps, cost-aware logging.
  - `MockAiContentGenerator`: deterministic offline generator used when
    `AI_PROVIDER=mock` (default for local dev and tests).
- **Policy firewall**: the AI never calculates billing, and its output can
  never override consent, suppression, authorization, or rate-limit
  decisions. Generated content is always human-reviewed before a campaign
  is scheduled (campaigns dispatch only in `SCHEDULED` via explicit user
  action).
- Prompt-injection safeguards: recipient/attribute values are treated as
  data, never appended to system prompts; generated HTML is sanitized
  before storage.

## Consequences

- (+) Provider-agnostic interface; mock mode enables offline demos and
  deterministic tests; no vendor SDK lock-in.
- (-) We do not use Spring AI (version-compatibility risk with the Boot
  3.5 line at the time of writing); the adapter is equivalent and
  documented. Re-evaluation trigger: when the project's Boot version is
  validated against a Spring AI GA release.

## Alternatives

- Spring AI: good abstraction, but BOM/Boot alignment was not verifiable
  against 3.5.16 at design time; deferred.
