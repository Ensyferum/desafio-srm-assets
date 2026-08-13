package com.srm.credit.dto;

import com.srm.credit.domain.Receivable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Representação pública de um recebível. */
public record ReceivableResponse(
        UUID id,
        UUID cedenteId,
        UUID receivableTypeId,
        String receivableTypeName,
        BigDecimal faceValue,
        LocalDate dueDate,
        String currency,
        String status,
        long version) {

    public static ReceivableResponse from(Receivable receivable) {
        return new ReceivableResponse(
                receivable.getId(),
                receivable.getCedenteId(),
                receivable.getType().getId(),
                receivable.getType().getName(),
                receivable.getFaceValue(),
                receivable.getDueDate(),
                receivable.getCurrency(),
                receivable.getStatus().name(),
                receivable.getVersion());
    }
}
