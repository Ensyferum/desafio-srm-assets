package com.srm.analytics.kafka;

import com.srm.analytics.repo.SettlementProjectionRepository;
import com.srm.common.event.SettlementEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consome {@code settlement.events} e materializa a projeção de leitura (CQRS/EDA): extrato de
 * liquidações + resumo diário por moeda.
 */
@Component
public class SettlementProjectionConsumer {

    public static final String TOPIC = "settlement.events";

    private static final Logger log = LoggerFactory.getLogger(SettlementProjectionConsumer.class);

    private final SettlementProjectionRepository projectionRepository;

    public SettlementProjectionConsumer(SettlementProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @KafkaListener(topics = TOPIC, groupId = "${app.kafka.consumer-group:analytics-projection}")
    public void onSettlement(SettlementEvent event) {
        log.info(
                "Projetando liquidação: transactionId={}, settlementCurrency={}",
                event.transactionId(),
                event.settlementCurrency());
        projectionRepository.upsert(event);
        projectionRepository.updateDailySummary(event);
    }
}
