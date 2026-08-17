package com.srm.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado no tópico {@code settlement.events} após a liquidação (ACID) de um recebível.
 * Consumido pelo analytics-service para construir projeções de leitura (CQRS/EDA).
 */
public record SettlementEvent(
        UUID transactionId,
        UUID receivableId,
        String cedenteDocument,
        BigDecimal faceValue,
        BigDecimal presentValue,
        BigDecimal discountValue,
        String currency,
        String settlementCurrency,
        BigDecimal exchangeRateApplied,
        Instant settledAt,
        String status) {}
