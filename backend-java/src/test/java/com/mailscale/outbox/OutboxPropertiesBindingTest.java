package com.mailscale.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Guards against a silent topic-binding regression: the outbox
 * publisher previously resolved a null topic because the property
 * field names did not match the configuration keys, which blocked
 * all Kafka publishing without failing any test.
 */
class OutboxPropertiesBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "mailscale.kafka.topic.email-commands=mailscale.emails.send",
                    "mailscale.kafka.topic.email-commands-dlq=mailscale.emails.send.dlq",
                    "mailscale.kafka.topic.delivery-events=mailscale.delivery.events",
                    "mailscale.kafka.topic.campaign-commands=mailscale.campaigns.dispatch",
                    "mailscale.kafka.topic.campaign-commands-dlq=mailscale.campaigns.dispatch.dlq"
            );

    @Test
    void relaxedBindingPopulatesEveryTopic() {
        runner.run(context -> {
            OutboxProperties properties =
                    context.getBean(OutboxProperties.class);

            assertThat(properties.getEmailCommands())
                    .isEqualTo("mailscale.emails.send");
            assertThat(properties.getEmailCommandsDlq())
                    .isEqualTo("mailscale.emails.send.dlq");
            assertThat(properties.getDeliveryEvents())
                    .isEqualTo("mailscale.delivery.events");
            assertThat(properties.getCampaignCommands())
                    .isEqualTo("mailscale.campaigns.dispatch");
            assertThat(properties.getCampaignCommandsDlq())
                    .isEqualTo("mailscale.campaigns.dispatch.dlq");
        });
    }

    @Test
    void missingValuesDoNotBindToDefaults() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class)
                .withPropertyValues(
                        "mailscale.kafka.topic.email-commands=only.this.one"
                )
                .run(context -> {
                    OutboxProperties properties =
                            context.getBean(OutboxProperties.class);

                    assertThat(properties.getEmailCommands())
                            .isEqualTo("only.this.one");
                    assertThat(properties.getCampaignCommands())
                            .isNull();
                });
    }

    @Configuration
    @EnableConfigurationProperties(OutboxProperties.class)
    static class TestConfig {
    }
}
