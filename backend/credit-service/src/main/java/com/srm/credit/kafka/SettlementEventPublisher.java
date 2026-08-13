package com.srm.credit.kafka;

import com.srm.common.event.SettlementEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica o evento {@code settlement.events} somente após o commit da liquidação (ACID),
 * alimentando a projeção de leitura do analytics-service.
 */
@Component
public class SettlementEventPublisher {

    public static final String TOPIC = "settlement.events";

    private static final Logger log = LoggerFactory.getLogger(SettlementEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SettlementEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSettlementCompleted(SettlementEvent event) {
        log.info(
                "Publicando settlement.events: transactionId={}, currency={}, PV={}",
                event.transactionId(),
                event.settlementCurrency(),
                event.presentValue());
        kafkaTemplate.send(TOPIC, event.transactionId().toString(), event);
    }
}
