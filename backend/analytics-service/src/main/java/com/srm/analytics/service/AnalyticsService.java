package com.srm.analytics.service;

import com.srm.analytics.dto.AnalyticsSummaryResponse;
import com.srm.analytics.dto.CedenteDistribution;
import com.srm.analytics.dto.TimeSeriesPoint;
import com.srm.analytics.repo.SettlementAnalyticsRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/** Resumo analítico de liquidações (camada de 2 níveis: controller → repository). */
@Service
public class AnalyticsService {

    private final SettlementAnalyticsRepository analyticsRepository;

    public AnalyticsService(SettlementAnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public AnalyticsSummaryResponse summary(LocalDate startDate, LocalDate endDate) {
        return analyticsRepository.summary(resolveStart(startDate), resolveEnd(endDate));
    }

    /** Série temporal diária de valor presente por moeda (dashboard). */
    public List<TimeSeriesPoint> timeSeries(LocalDate startDate, LocalDate endDate) {
        return analyticsRepository.timeSeries(resolveStart(startDate), resolveEnd(endDate));
    }

    /** Distribuição das liquidações por cedente (dashboard). */
    public List<CedenteDistribution> distributionByCedente(LocalDate startDate, LocalDate endDate) {
        return analyticsRepository.distributionByCedente(
                resolveStart(startDate), resolveEnd(endDate));
    }

    private static LocalDate resolveStart(LocalDate startDate) {
        return startDate == null ? LocalDate.now().minusDays(30) : startDate;
    }

    private static LocalDate resolveEnd(LocalDate endDate) {
        return endDate == null ? LocalDate.now() : endDate;
    }
}
