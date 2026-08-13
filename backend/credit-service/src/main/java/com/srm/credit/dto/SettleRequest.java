package com.srm.credit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Liquidação de recebível (RF03). */
public record SettleRequest(
        @NotBlank(message = "settlementCurrency é obrigatório")
                @Pattern(regexp = "BRL|USD", message = "settlementCurrency deve ser BRL ou USD")
                String settlementCurrency) {}
