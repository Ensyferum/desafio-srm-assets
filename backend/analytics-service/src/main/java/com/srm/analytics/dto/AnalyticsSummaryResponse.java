package com.srm.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Resumo analítico das liquidações por período. */
public record AnalyticsSummaryResponse(
        long totalTransactions,
        BigDecimal totalPresentValue,
        BigDecimal totalDiscountValue,
        Map<String, BigDecimal> presentValueByCurrency,
        LocalDate startDate,
        LocalDate endDate) {}
