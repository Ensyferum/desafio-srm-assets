package com.srm.common.correlation;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;

/**
 * Interceptor Kafka que:
 *
 * <ul>
 *   <li>no produtor — adiciona o correlation id do MDC no header da mensagem;
 *   <li>no consumidor — restaura o correlation id do header no MDC do thread do listener.
 * </ul>
 */
public class CorrelationIdKafkaInterceptor
        implements ProducerInterceptor<String, Object>, ConsumerInterceptor<String, Object> {

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        String correlationId = CorrelationIds.get();
        if (correlationId != null && record.headers().lastHeader(CorrelationIds.HEADER) == null) {
            record.headers()
                    .add(CorrelationIds.HEADER, correlationId.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }

    @Override
    public ConsumerRecords<String, Object> onConsume(ConsumerRecords<String, Object> records) {
        CorrelationIds.clear();
        if (!records.isEmpty()) {
            ConsumerRecord<String, Object> first = records.iterator().next();
            Header header = first.headers().lastHeader(CorrelationIds.HEADER);
            if (header != null) {
                CorrelationIds.set(new String(header.value(), StandardCharsets.UTF_8));
            }
        }
        return records;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {}

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {}

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}
