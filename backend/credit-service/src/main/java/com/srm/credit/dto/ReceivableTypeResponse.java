package com.srm.credit.dto;

import com.srm.credit.domain.ReceivableType;
import java.math.BigDecimal;
import java.util.UUID;

/** Representação pública de um tipo de recebível. */
public record ReceivableTypeResponse(
        UUID id, String name, BigDecimal spreadMonthly, String description) {

    public static ReceivableTypeResponse from(ReceivableType type) {
        return new ReceivableTypeResponse(
                type.getId(), type.getName(), type.getSpreadMonthly(), type.getDescription());
    }
}
