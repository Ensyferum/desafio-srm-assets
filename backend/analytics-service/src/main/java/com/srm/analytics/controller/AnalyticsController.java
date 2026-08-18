package com.srm.analytics.controller;

import com.srm.analytics.dto.AnalyticsSummaryResponse;
import com.srm.analytics.dto.CedenteDistribution;
import com.srm.analytics.dto.TimeSeriesPoint;
import com.srm.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Resumo analítico para o dashboard. */
@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Resumo de liquidações por período e moeda")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumo de liquidações do período")
    public AnalyticsSummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate) {
        return analyticsService.summary(startDate, endDate);
    }

    @GetMapping("/timeseries")
    @Operation(summary = "Série temporal diária de valor presente por moeda")
    public List<TimeSeriesPoint> timeSeries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate) {
        return analyticsService.timeSeries(startDate, endDate);
    }

    @GetMapping("/by-cedente")
    @Operation(summary = "Distribuição das liquidações por cedente (CNPJ)")
    public List<CedenteDistribution> byCedente(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate) {
        return analyticsService.distributionByCedente(startDate, endDate);
    }
}
