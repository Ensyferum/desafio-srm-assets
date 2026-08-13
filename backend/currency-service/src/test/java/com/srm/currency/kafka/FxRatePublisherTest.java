package com.srm.currency.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.srm.common.event.FxUpdatedEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class FxRatePublisherTest {

    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void publishesFxUpdatedEventToTopic() {
        FxRatePublisher publisher = new FxRatePublisher(kafkaTemplate);
        FxUpdatedEvent event =
                new FxUpdatedEvent(
                        "USD", "BRL", new BigDecimal("5.4523"), LocalDate.of(2026, 8, 12), "test");

        publisher.onFxUpdated(event);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(FxRatePublisher.TOPIC), eq("USD-BRL"), captor.capture());
        assertThat(captor.getValue()).isSameAs(event);
    }

    @Test
    void topicNameMatchesManagedTopic() {
        assertThat(FxRatePublisher.TOPIC).isEqualTo("fx.updated");
        KafkaConfig config = new KafkaConfig();
        assertThat(config.fxUpdatedTopic().name()).isEqualTo("fx.updated");
        assertThat(config.fxUpdatedTopic().numPartitions()).isEqualTo(3);
        assertThat(config.fxUpdatedTopic().replicationFactor()).isEqualTo((short) 1);
    }
}
