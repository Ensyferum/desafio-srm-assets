package com.srm.credit.fx;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Contrato de resposta do currency-service (GET /api/v1/exchange-rates). */
public record ExchangeRateResponse(
        String fromCurrency, String toCurrency, BigDecimal rate, LocalDate effectiveDate) {}
