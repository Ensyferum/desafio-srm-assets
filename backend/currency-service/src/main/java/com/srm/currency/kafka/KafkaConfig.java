package com.srm.currency.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Tópicos Kafka gerenciados pelo currency-service. */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic fxUpdatedTopic() {
        return TopicBuilder.name(FxRatePublisher.TOPIC).partitions(3).replicas(1).build();
    }
}
