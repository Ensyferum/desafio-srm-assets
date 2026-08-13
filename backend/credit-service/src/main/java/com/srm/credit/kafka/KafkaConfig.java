package com.srm.credit.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Tópicos Kafka gerenciados pelo credit-service. */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic settlementEventsTopic() {
        return TopicBuilder.name(SettlementEventPublisher.TOPIC).partitions(3).replicas(1).build();
    }
}
