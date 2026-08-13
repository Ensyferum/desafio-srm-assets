package com.srm.credit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Simulação de precificação em tempo real (RF02). */
public record PriceSimulationRequest(
        @NotNull(message = "faceValue é obrigatório")
                @DecimalMin(value = "0.01", message = "faceValue deve ser maior que zero")
                BigDecimal faceValue,
        @NotNull(message = "dueDate é obrigatório") @Future(message = "dueDate deve ser futura")
                LocalDate dueDate,
        @NotNull(message = "receivableTypeId é obrigatório") UUID receivableTypeId,
        @NotNull(message = "currency é obrigatório")
                @Pattern(regexp = "BRL|USD", message = "currency deve ser BRL ou USD")
                String currency,
        @NotNull(message = "settlementCurrency é obrigatório")
                @Pattern(regexp = "BRL|USD", message = "settlementCurrency deve ser BRL ou USD")
                String settlementCurrency,
        @DecimalMin(value = "0", message = "baseRate não pode ser negativa") BigDecimal baseRate) {}
