package com.srm.analytics.service;

import com.srm.analytics.dto.AnalyticsSummaryResponse;
import com.srm.analytics.repo.SettlementAnalyticsRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/** Resumo analítico de liquidações (camada de 2 níveis: controller → repository). */
@Service
public class AnalyticsService {

    private final SettlementAnalyticsRepository analyticsRepository;

    public AnalyticsService(SettlementAnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public AnalyticsSummaryResponse summary(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        return analyticsRepository.summary(start, end);
    }
}
