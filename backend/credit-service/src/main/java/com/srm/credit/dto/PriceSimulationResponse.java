package com.srm.credit.dto;

import com.srm.credit.pricing.PricingResult;
import java.math.BigDecimal;

/** Resultado da simulação de precificação. */
public record PriceSimulationResponse(
        BigDecimal faceValue,
        BigDecimal presentValue,
        BigDecimal discountValue,
        BigDecimal spreadApplied,
        BigDecimal termMonths,
        BigDecimal baseRate,
        BigDecimal exchangeRateApplied,
        BigDecimal presentValueInSettlementCurrency,
        String currency,
        String settlementCurrency,
        String receivableTypeName) {

    public static PriceSimulationResponse from(PricingResult result) {
        return new PriceSimulationResponse(
                result.faceValue(),
                result.presentValue(),
                result.discountValue(),
                result.spreadApplied(),
                result.termMonths(),
                result.baseRate(),
                result.exchangeRateApplied(),
                result.presentValueInSettlementCurrency(),
                result.currency(),
                result.settlementCurrency(),
                result.receivableTypeName());
    }
}
