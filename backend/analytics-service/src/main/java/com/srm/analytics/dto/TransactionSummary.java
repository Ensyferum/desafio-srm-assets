package com.srm.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Linha do extrato de liquidações (RF05). */
public record TransactionSummary(
        UUID transactionId,
        UUID receivableId,
        UUID cedenteId,
        BigDecimal faceValue,
        BigDecimal presentValue,
        BigDecimal discountValue,
        String currency,
        String settlementCurrency,
        BigDecimal exchangeRateApplied,
        String status,
        Instant settledAt) {}
