package com.srm.currency.kafka;

import com.srm.common.event.FxUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica o evento {@code fx.updated} somente após o commit da transação, garantindo que o evento
 * reflita um estado já persistido (EDA).
 */
@Component
public class FxRatePublisher {

    public static final String TOPIC = "fx.updated";

    private static final Logger log = LoggerFactory.getLogger(FxRatePublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FxRatePublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFxUpdated(FxUpdatedEvent event) {
        log.info(
                "Publicando evento fx.updated: {} → {} = {} em {}",
                event.fromCurrency(),
                event.toCurrency(),
                event.rate(),
                event.effectiveDate());
        kafkaTemplate.send(TOPIC, event.fromCurrency() + "-" + event.toCurrency(), event);
    }
}
