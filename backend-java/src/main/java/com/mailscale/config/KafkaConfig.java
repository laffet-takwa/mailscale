package com.mailscale.config;

import com.mailscale.common.CorrelationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(
            @Value("${mailscale.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers);
        return new KafkaAdmin(props);
    }

    @Bean
    public NewTopic emailCommandsTopic(
            @Value("${mailscale.kafka.topic.email-commands}") String name) {
        return new NewTopic(name, 6, (short) 1);
    }

    @Bean
    public NewTopic emailCommandsDlq(
            @Value("${mailscale.kafka.topic.email-commands-dlq}") String name) {
        return new NewTopic(name, 6, (short) 1);
    }

    @Bean
    public NewTopic deliveryEventsTopic(
            @Value("${mailscale.kafka.topic.delivery-events}") String name) {
        return new NewTopic(name, 6, (short) 1);
    }

    @Bean
    public NewTopic campaignCommandsTopic(
            @Value("${mailscale.kafka.topic.campaign-commands}") String name) {
        return new NewTopic(name, 6, (short) 1);
    }

    @Bean
    public NewTopic campaignCommandsDlq(
            @Value("${mailscale.kafka.topic.campaign-commands-dlq}") String name) {
        return new NewTopic(name, 6, (short) 1);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(
            @Value("${mailscale.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG, "all");
        props.put(org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                true);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
                5);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        return template;
    }
}
