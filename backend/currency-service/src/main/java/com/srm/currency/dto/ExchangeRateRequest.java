package com.srm.currency.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Criação/atualização de taxa de câmbio (RF01). */
public record ExchangeRateRequest(
        @NotBlank(message = "fromCurrency é obrigatório")
                @Size(min = 3, max = 3, message = "fromCurrency deve ter 3 caracteres")
                String fromCurrency,
        @NotBlank(message = "toCurrency é obrigatório")
                @Size(min = 3, max = 3, message = "toCurrency deve ter 3 caracteres")
                String toCurrency,
        @NotNull(message = "rate é obrigatório")
                @DecimalMin(value = "0.0000000001", message = "rate deve ser maior que zero")
                @Digits(integer = 10, fraction = 10, message = "rate com precisão inválida")
                BigDecimal rate,
        @NotNull(message = "effectiveDate é obrigatório") LocalDate effectiveDate) {}
