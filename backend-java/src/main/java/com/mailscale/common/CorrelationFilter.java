package com.mailscale.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ensures every request carries a correlation ID: either the client-supplied
 * X-Correlation-ID or a generated UUID. The ID is placed in MDC for structured
 * logging and into the response header for clients.
 */
@Component
public class CorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString().replace("-", "");
        }
        CorrelationHolder.set(correlationId);
        MDC.put("correlationId", correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            CorrelationHolder.clear();
            MDC.remove("correlationId");
        }
    }
}
