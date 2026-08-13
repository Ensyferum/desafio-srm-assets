package com.srm.credit.pricing;

import java.math.BigDecimal;

/**
 * Resultado do cálculo de precificação com valor presente, deságio e conversão cambial quando a
 * liquidação é cross-currency.
 */
public record PricingResult(
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
        String receivableTypeName) {}
