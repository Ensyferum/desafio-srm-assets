package com.srm.credit.dto;

import java.util.List;

/** Resposta da criação em lote. */
public record CreateReceivablesBatchResponse(int created, List<ReceivableResponse> receivables) {}
