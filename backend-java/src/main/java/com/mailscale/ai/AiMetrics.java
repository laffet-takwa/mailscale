package com.mailscale.ai;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Micrometer metrics for the AI assistant: request count, latency, and
 * failures by provider. Cost-aware logging (no prompt or response text)
 * lives in the generators themselves.
 */
@Component
public class AiMetrics {

    private final Counter requests;
    private final Counter failures;
    private final Timer latency;

    public AiMetrics(MeterRegistry registry) {
        this.requests = Counter.builder("mailscale_ai_requests_total")
                .description("AI generation requests")
                .register(registry);
        this.failures = Counter.builder(
                "mailscale_ai_requests_failed_total")
                .description("AI generation failures")
                .register(registry);
        this.latency = Timer.builder(
                "mailscale_ai_request_duration_seconds")
                .description("AI generation latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void recordStart() {
        requests.increment();
    }

    public void recordFailure() {
        failures.increment();
    }

    public Timer.Sample startTimer(MeterRegistry registry) {
        return Timer.start(registry);
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(latency);
    }
}
