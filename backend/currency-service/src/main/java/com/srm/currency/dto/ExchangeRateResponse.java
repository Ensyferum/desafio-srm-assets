package com.srm.currency.dto;

import com.srm.currency.domain.ExchangeRate;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Representação pública de uma taxa de câmbio. */
public record ExchangeRateResponse(
        String fromCurrency, String toCurrency, BigDecimal rate, LocalDate effectiveDate) {

    public static ExchangeRateResponse from(ExchangeRate rate) {
        return new ExchangeRateResponse(
                rate.getFromCurrency(),
                rate.getToCurrency(),
                rate.getRate(),
                rate.getEffectiveDate());
    }
}
