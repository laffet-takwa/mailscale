package com.mailscale.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Writes a JSON 401 response with the same error schema as the REST
 * layer, so authentication failures never leak a stack trace or a
 * Spring-specific payload.
 *
 * <p>The correlation ID is read from the MDC, which the correlation
 * filter populates for every request; it is also echoed in the
 * response header.
 */
@Component
public final class UnauthorizedHandler {

    private final ObjectMapper objectMapper;

    public UnauthorizedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        String correlationId = MDC.get("correlationId");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "UNAUTHORIZED");
        body.put("message", message);
        body.put("path", request.getRequestURI());
        body.put("correlationId",
                correlationId == null ? "" : correlationId);
        response.setHeader("X-Correlation-ID",
                correlationId == null ? "" : correlationId);

        objectMapper.writeValue(response.getWriter(), body);
    }
}
