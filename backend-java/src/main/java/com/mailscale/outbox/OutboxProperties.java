package com.mailscale.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mailscale.kafka.topic")
public class OutboxProperties {

    private String emailCommandsTopic;
    private String emailCommandsDlq;
    private String deliveryEventsTopic;
    private String campaignCommandsTopic;
    private String campaignCommandsDlq;

    public String getEmailCommandsTopic() {
        return emailCommandsTopic;
    }

    public void setEmailCommandsTopic(String emailCommandsTopic) {
        this.emailCommandsTopic = emailCommandsTopic;
    }

    public String getEmailCommandsDlq() {
        return emailCommandsDlq;
    }

    public void setEmailCommandsDlq(String emailCommandsDlq) {
        this.emailCommandsDlq = emailCommandsDlq;
    }

    public String getDeliveryEventsTopic() {
        return deliveryEventsTopic;
    }

    public void setDeliveryEventsTopic(String deliveryEventsTopic) {
        this.deliveryEventsTopic = deliveryEventsTopic;
    }

    public String getCampaignCommandsTopic() {
        return campaignCommandsTopic;
    }

    public void setCampaignCommandsTopic(String campaignCommandsTopic) {
        this.campaignCommandsTopic = campaignCommandsTopic;
    }

    public String getCampaignCommandsDlq() {
        return campaignCommandsDlq;
    }

    public void setCampaignCommandsDlq(String campaignCommandsDlq) {
        this.campaignCommandsDlq = campaignCommandsDlq;
    }
}
