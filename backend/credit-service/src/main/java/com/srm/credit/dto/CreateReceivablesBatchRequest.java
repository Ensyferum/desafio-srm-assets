package com.srm.credit.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Lote de recebíveis (criação em massa). */
public record CreateReceivablesBatchRequest(
        @NotEmpty(message = "informe ao menos um recebível") @Valid
                List<CreateReceivableRequest> receivables) {}
