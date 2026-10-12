package com.mailscale.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailscale.common.CorrelationHolder;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Publishes unpublished outbox events to Kafka.
 *
 * <p>Claiming is done with a conditional UPDATE so multiple API instances can
 * run the publisher concurrently without double-publishing (ADR-0003). The
 * claim is best-effort: at-least-once publishing with idempotent consumers.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxProperties properties;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                             KafkaTemplate<String, Object> kafkaTemplate,
                             ObjectMapper objectMapper,
                             OutboxProperties properties) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${mailscale.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> candidates = outboxEventRepository.findUnpublished();
        if (candidates.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        int published = 0;
        for (OutboxEvent event : candidates) {
            int claimed = outboxEventRepository.claim(event.getId(), now);
            if (claimed == 0) {
                continue; // claimed by another instance or already published
            }
            try {
                String topic = topicFor(event.getEventType());
                String key = event.getAggregateId();
                kafkaTemplate.send(topic, key, deserialize(event.getPayload()))
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish outbox event {}: {}",
                                        event.getId(), ex.getMessage());
                            } else {
                                log.debug("Published outbox event {} to {} partition {}",
                                        event.getId(), topic,
                                        result != null ? result.getRecordMetadata()
                                                .partition() : "n/a");
                            }
                        }).get();
                outboxEventRepository.markPublished(event.getId(), Instant.now());
                published++;
            } catch (Exception e) {
                log.warn("Could not publish outbox event {}, will retry: {}",
                        event.getId(), e.getMessage());
                outboxEventRepository.releaseLock(event.getId());
            }
        }
        if (published > 0) {
            log.info("Published {} outbox events", published);
        }
    }

    @Scheduled(fixedDelayString = "${mailscale.outbox.cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanupPublished() {
        int deleted = outboxEventRepository.deletePublishedBefore(
                Instant.now().minus(java.time.Duration.ofDays(7)));
        if (deleted > 0) {
            log.info("Cleaned up {} published outbox events", deleted);
        }
    }

    private String topicFor(String eventType) {
        if ("EMAIL_QUEUED".equals(eventType)
                || "CAMPAIGN_EMAIL_QUEUED".equals(eventType)) {
            return properties.getEmailCommands();
        }
        if ("CAMPAIGN_DISPATCH".equals(eventType)) {
            return properties.getCampaignCommands();
        }
        throw new IllegalArgumentException("Unknown event type: " + eventType);
    }

    private Object deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid outbox payload", e);
        }
    }
}
