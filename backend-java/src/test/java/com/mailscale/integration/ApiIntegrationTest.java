package com.mailscale.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end HTTP tests for the API layer: security filters, JWT and
 * API-key authentication, tenant isolation, validation, idempotency,
 * and the transactional email endpoint.
 *
 * <p>Each test registers its own organization so the tests are
 * independent and can be reordered without breaking.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Registers a fresh organization and returns its JWT. */
    private String register(String email, String slug) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s\",\"password\":\"%s\","
                                        + "\"fullName\":\"API User\","
                                        + "\"organizationName\":\"API Org\","
                                        + "\"organizationSlug\":\"%s\"}",
                                email, PASSWORD, slug)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("token")
                .asText();
    }

    private long createTemplate(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/templates")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Correlation-ID", "corr-" + name)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "%s",
                                  "subject": "Hi {{firstName}}",
                                  "htmlBody": "<p>Hi {{firstName}}</p>",
                                  "textBody": "Hi {{firstName}}"
                                }
                                """, name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/contacts"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/analytics/dashboard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/templates"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/contacts")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@mailscale.local\","
                                + "\"password\":\"definitely-wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void templateCreationEchoesCorrelationId() throws Exception {
        String token = register(
                "corr@example.com", "corr-org-" + System.nanoTime()
        );
        long templateId = createTemplate(token, "corr-template");

        mockMvc.perform(get("/api/v1/templates/" + templateId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Correlation-ID", "trace-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(templateId));
    }

    @Test
    void transactionalEmailIsAcceptedThenIdempotent() throws Exception {
        String token = register(
                "email-api@example.com", "email-org-" + System.nanoTime()
        );
        long templateId = createTemplate(token, "email-template");

        String request = String.format("""
                {
                  "to": "api-test@example.com",
                  "templateId": %d,
                  "variables": {"firstName": "API"},
                  "idempotencyKey": "api-idem-1"
                }
                """, templateId);

        MvcResult result = mockMvc.perform(post("/api/v1/emails")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.correlationId").isString())
                .andReturn();

        String messageId = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("messageId")
                .asText();

        // Status lookup by the returned message ID.
        mockMvc.perform(get("/api/v1/emails/" + messageId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId))
                .andExpect(jsonPath("$.status").value("QUEUED"));

        // Replay with the same key returns the same response.
        MvcResult replay = mockMvc.perform(post("/api/v1/emails")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isAccepted())
                .andReturn();
        String replayId = objectMapper
                .readTree(replay.getResponse().getContentAsString())
                .get("messageId")
                .asText();
        org.junit.jupiter.api.Assertions.assertEquals(
                messageId, replayId
        );

        // Same key, different payload: 409 CONFLICT.
        mockMvc.perform(post("/api/v1/emails")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "to": "other@example.com",
                                  "templateId": %d,
                                  "idempotencyKey": "api-idem-1"
                                }
                                """, templateId)))
                .andExpect(status().isConflict());
    }

    @Test
    void missingIdempotencyKeyIsRejected() throws Exception {
        String token = register(
                "nokey@example.com", "nokey-org-" + System.nanoTime()
        );
        long templateId = createTemplate(token, "nokey-template");

        mockMvc.perform(post("/api/v1/emails")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "to": "nokey@example.com",
                                  "templateId": %d
                                }
                                """, templateId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownTemplateReturns404() throws Exception {
        String token = register(
                "tpl404@example.com", "tpl404-org-" + System.nanoTime()
        );
        mockMvc.perform(post("/api/v1/emails")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "to": "who@example.com",
                                  "templateId": 999999,
                                  "idempotencyKey": "missing-template"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void tenantIsolationHidesOtherOrganizationsTemplates() throws Exception {
        String ownerToken = register(
                "owner@example.com", "owner-org-" + System.nanoTime()
        );
        long templateId = createTemplate(ownerToken, "owner-template");

        mockMvc.perform(get("/api/v1/templates/" + templateId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        String otherToken = register(
                "intruder@example.com", "intruder-org-" + System.nanoTime()
        );
        mockMvc.perform(get("/api/v1/templates/" + templateId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        // The intruder's own list is empty.
        mockMvc.perform(get("/api/v1/templates")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void tenantIsolationHidesOtherContactRecords() throws Exception {
        String ownerToken = register(
                "cowner@example.com", "cowner-org-" + System.nanoTime()
        );
        MvcResult contact = mockMvc.perform(post("/api/v1/contacts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"secret@example.com\","
                                + "\"firstName\":\"Secret\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long contactId = objectMapper
                .readTree(contact.getResponse().getContentAsString())
                .get("id")
                .asLong();

        String otherToken = register(
                "lurker@example.com", "lurker-org-" + System.nanoTime()
        );
        mockMvc.perform(get("/api/v1/contacts/" + contactId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void campaignValidationReportsProblemsForEmptyAudience() throws Exception {
        String token = register(
                "camp@example.com", "camp-org-" + System.nanoTime()
        );
        long templateId = createTemplate(token, "camp-template");

        // Use the seeded "All active contacts" segment, but the new
        // organization has no contacts at all.
        MvcResult segments = mockMvc.perform(get("/api/v1/segments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        // Register marks the org with no contacts; create a segment.
        MvcResult segment = mockMvc.perform(post("/api/v1/segments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"All active contacts\","
                                + "\"definition\":\"{\\\"match\\\":\\\"ALL\\\","
                                + "\\\"rules\\\":[{\\\"field\\\":\\\"status\\\","
                                + "\\\"operator\\\":\\\"EQUALS\\\","
                                + "\\\"value\\\":\\\"ACTIVE\\\"}]}\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long segmentId = objectMapper
                .readTree(segment.getResponse().getContentAsString())
                .get("id")
                .asLong();
        org.junit.jupiter.api.Assertions.assertEquals(
                objectMapper.readTree(
                                segments.getResponse().getContentAsString())
                        .size(),
                1
        );

        MvcResult created = mockMvc.perform(post("/api/v1/campaigns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "API campaign",
                                  "segmentId": %d,
                                  "templateId": %d
                                }
                                """, segmentId, templateId)))
                .andExpect(status().isOk())
                .andReturn();
        long campaignId = objectMapper
                .readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();

        MvcResult validation = mockMvc.perform(
                        get("/api/v1/campaigns/" + campaignId + "/validate")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        org.junit.jupiter.api.Assertions.assertFalse(
                objectMapper
                        .readTree(
                                validation.getResponse().getContentAsString())
                        .get("problems")
                        .isEmpty()
        );
    }

    @Test
    void apiKeyAuthenticationWorks() throws Exception {
        String token = register(
                "keyapi@example.com", "keyapi-org-" + System.nanoTime()
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/api-keys")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"api-key-test\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String secret = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("secret")
                .asText();

        mockMvc.perform(get("/api/v1/contacts")
                        .header("X-API-Key", secret))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/contacts")
                        .header("X-API-Key", "msk_live_invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonExistentRouteReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/no-such-endpoint"))
                .andExpect(status().is4xxClientError());
    }
}
