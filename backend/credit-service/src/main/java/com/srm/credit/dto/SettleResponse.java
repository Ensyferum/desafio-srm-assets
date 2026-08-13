package com.srm.credit.dto;

import com.srm.credit.domain.Transaction;
import com.srm.credit.pricing.PricingResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Resposta da liquidação de um recebível. */
public record SettleResponse(
        UUID transactionId,
        String status,
        BigDecimal presentValue,
        BigDecimal discountValue,
        String settlementCurrency,
        BigDecimal exchangeRateApplied,
        BigDecimal presentValueInSettlementCurrency,
        Instant settledAt) {

    public static SettleResponse from(Transaction transaction, PricingResult pricing) {
        return new SettleResponse(
                transaction.getId(),
                transaction.getStatus().name(),
                transaction.getPresentValue(),
                transaction.getDiscountValue(),
                transaction.getSettlementCurrency(),
                transaction.getExchangeRateApplied(),
                pricing.presentValueInSettlementCurrency(),
                transaction.getSettledAt());
    }
}
