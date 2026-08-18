package com.srm.analytics.dto;

import java.math.BigDecimal;

/** Distribuição das liquidações por cedente (CNPJ) no período. */
public record CedenteDistribution(
        String cedenteDocument, long transactions, BigDecimal presentValue) {}
