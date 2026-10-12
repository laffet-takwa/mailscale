package com.mailscale.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailscale.outbox.OutboxProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes delivery events published by the Kotlin worker
 * and updates the corresponding email message status.
 *
 * <p>The worker is the source of truth for delivery outcomes;
 * this consumer reconciles the API's message table. Updates
 * are idempotent: terminal statuses (DELIVERED, FAILED,
 * SUPPRESSED) are never overwritten by later events.
 */
@Component
public class DeliveryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(
            DeliveryEventConsumer.class);

    private final EmailMessageRepository emailMessageRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final ObjectMapper objectMapper;

    public DeliveryEventConsumer(
            EmailMessageRepository emailMessageRepository,
            DeliveryAttemptRepository deliveryAttemptRepository,
            ObjectMapper objectMapper) {
        this.emailMessageRepository = emailMessageRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${mailscale.kafka.topic.delivery-events}",
            groupId = "mailscale-api-analytics",
            containerFactory = "kafkaListenerContainerFactory")
    public void onDeliveryEvent(ConsumerRecord<String, String> record,
                                    Acknowledgment ack) {
        try {
            JsonNode event = objectMapper.readTree(
                    record.value());
            UUID messageId = UUID.fromString(
                    event.get("messageId").asText());
            String status = event.get("status").asText();
            String providerMessageId = event.has("providerMessageId")
                    && !event.get("providerMessageId").isNull()
                    ? event.get("providerMessageId").asText() : null;

            emailMessageRepository.findById(messageId).ifPresent(
                    message -> {
                        applyStatus(message, status,
                                providerMessageId);
                        emailMessageRepository.save(message);
                    });
            log.debug("Applied delivery event messageId={} status={}",
                    messageId, status);
        } catch (Exception e) {
            log.error("Failed to process delivery event: {}",
                    e.getMessage());
        } finally {
            if (ack != null) {
                ack.acknowledge();
            }
        }
    }

    private void applyStatus(EmailMessage message, String status,
                                 String providerMessageId) {
        switch (status) {
            case "SENT" -> message.markSent(providerMessageId);
            case "DELIVERED" -> message.markDelivered(
                    providerMessageId);
            case "BOUNCED" -> message.markBounced(providerMessageId);
            case "FAILED" -> message.markFailed();
            case "SUPPRESSED" -> message.markSuppressed();
            default -> log.warn("Unknown delivery status: {}",
                    status);
        }
    }
}
