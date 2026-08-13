package com.srm.currency.dto;

import com.srm.currency.domain.Currency;
import java.util.UUID;

/** Representação pública de uma moeda. */
public record CurrencyResponse(UUID id, String code, String name, String symbol) {

    public static CurrencyResponse from(Currency currency) {
        return new CurrencyResponse(
                currency.getId(), currency.getCode(), currency.getName(), currency.getSymbol());
    }
}
