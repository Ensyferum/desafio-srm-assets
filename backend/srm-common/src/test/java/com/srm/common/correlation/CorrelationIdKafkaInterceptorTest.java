package com.srm.common.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

class CorrelationIdKafkaInterceptorTest {

    private final CorrelationIdKafkaInterceptor interceptor = new CorrelationIdKafkaInterceptor();

    @Test
    void addsCorrelationIdHeaderOnSend() {
        CorrelationIds.set("cid-kafka-producer");

        ProducerRecord<String, Object> record =
                new ProducerRecord<>("settlement.events", "key", new Object());
        ProducerRecord<String, Object> sent = interceptor.onSend(record);

        assertThat(sent.headers().lastHeader(CorrelationIds.HEADER)).isNotNull();
        assertThat(
                        new String(
                                sent.headers().lastHeader(CorrelationIds.HEADER).value(),
                                StandardCharsets.UTF_8))
                .isEqualTo("cid-kafka-producer");
    }

    @Test
    void restoresCorrelationIdIntoMdcOnConsume() {
        ConsumerRecord<String, Object> record =
                new ConsumerRecord<>("settlement.events", 0, 0L, "key", "value");
        record.headers()
                .add(CorrelationIds.HEADER, "cid-kafka-consumer".getBytes(StandardCharsets.UTF_8));
        ConsumerRecords<String, Object> records =
                new ConsumerRecords<>(
                        Map.of(new TopicPartition("settlement.events", 0), List.of(record)));

        interceptor.onConsume(records);

        assertThat(CorrelationIds.get()).isEqualTo("cid-kafka-consumer");
        CorrelationIds.clear();
    }

    @Test
    void clearsMdcWhenRecordHasNoCorrelationId() {
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("t", 0, 0L, "k", "v");
        ConsumerRecords<String, Object> records =
                new ConsumerRecords<>(Map.of(new TopicPartition("t", 0), List.of(record)));

        CorrelationIds.set("stale");
        interceptor.onConsume(records);

        assertThat(CorrelationIds.get()).isNull();
    }
}
