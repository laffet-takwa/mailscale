package com.mailscale.monitoring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.SystemHealth;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Readiness and liveness indicators exposed via the actuator
 * and a simplified status endpoint for the frontend.
 */
@RestController
@RequestMapping("/api/v1/monitoring")
@Tag(name = "Monitoring")
public class MonitoringController {

    private final HealthEndpoint healthEndpoint;

    public MonitoringController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/status")
    @Operation(summary = "Simplified system status for the frontend")
    public ResponseEntity<Map<String, Object>> status() {
        // healthForPath("") returns null when no health group matches;
        // fall back to the aggregate health endpoint instead of throwing.
        HealthComponent overallHealth = healthEndpoint.healthForPath();
        if (overallHealth == null) {
            overallHealth = healthEndpoint.health();
        }
        Map<String, Object> components = new LinkedHashMap<>();
        if (overallHealth instanceof SystemHealth systemHealth) {
            for (Map.Entry<String, HealthComponent> entry
                    : systemHealth.getComponents().entrySet()) {
                components.put(entry.getKey(),
                        entry.getValue().getStatus().getCode());
            }
        }
        return ResponseEntity.ok(Map.of(
                "overall", overallHealth.getStatus().getCode(),
                "components", components));
    }
}
