package com.srm.currency.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KafkaConfigTest {

    @Test
    void managesFxUpdatedTopicWithThreePartitions() {
        KafkaConfig config = new KafkaConfig();

        assertThat(config.fxUpdatedTopic().name()).isEqualTo("fx.updated");
        assertThat(config.fxUpdatedTopic().numPartitions()).isEqualTo(3);
        assertThat(config.fxUpdatedTopic().replicationFactor()).isEqualTo((short) 1);
    }
}
