package com.srm.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Ponto de série temporal: valor presente agregado por dia e moeda (dashboard). */
public record TimeSeriesPoint(
        LocalDate date, String currency, long transactions, BigDecimal presentValue) {}
