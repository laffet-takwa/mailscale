package com.mailscale.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kafka topic names bound from {@code mailscale.kafka.topic.*}.
 *
 * <p>Relaxed binding maps {@code email-commands} to
 * {@code emailCommands}, so the field names deliberately omit the
 * "Topic" suffix to match the configuration keys.
 */
@Component
@ConfigurationProperties(prefix = "mailscale.kafka.topic")
public class OutboxProperties {

    private String emailCommands;
    private String emailCommandsDlq;
    private String deliveryEvents;
    private String campaignCommands;
    private String campaignCommandsDlq;

    public String getEmailCommands() {
        return emailCommands;
    }

    public void setEmailCommands(String emailCommands) {
        this.emailCommands = emailCommands;
    }

    public String getEmailCommandsDlq() {
        return emailCommandsDlq;
    }

    public void setEmailCommandsDlq(String emailCommandsDlq) {
        this.emailCommandsDlq = emailCommandsDlq;
    }

    public String getDeliveryEvents() {
        return deliveryEvents;
    }

    public void setDeliveryEvents(String deliveryEvents) {
        this.deliveryEvents = deliveryEvents;
    }

    public String getCampaignCommands() {
        return campaignCommands;
    }

    public void setCampaignCommands(String campaignCommands) {
        this.campaignCommands = campaignCommands;
    }

    public String getCampaignCommandsDlq() {
        return campaignCommandsDlq;
    }

    public void setCampaignCommandsDlq(String campaignCommandsDlq) {
        this.campaignCommandsDlq = campaignCommandsDlq;
    }
}
