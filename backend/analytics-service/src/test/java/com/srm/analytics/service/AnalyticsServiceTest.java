package com.srm.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.analytics.dto.AnalyticsSummaryResponse;
import com.srm.analytics.repo.SettlementAnalyticsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnalyticsServiceTest {

    @Test
    void delegatesToRepositoryWithExplicitDates() {
        SettlementAnalyticsRepository repository = mock(SettlementAnalyticsRepository.class);
        AnalyticsService service = new AnalyticsService(repository);
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        AnalyticsSummaryResponse expected =
                new AnalyticsSummaryResponse(
                        2,
                        new BigDecimal("100.00"),
                        new BigDecimal("10.00"),
                        Map.of("BRL", new BigDecimal("100.00")),
                        start,
                        end);
        when(repository.summary(start, end)).thenReturn(expected);

        AnalyticsSummaryResponse result = service.summary(start, end);

        assertThat(result).isEqualTo(expected);
        verify(repository).summary(start, end);
    }

    @Test
    void defaultsMissingDatesToLast30Days() {
        SettlementAnalyticsRepository repository = mock(SettlementAnalyticsRepository.class);
        AnalyticsService service = new AnalyticsService(repository);
        when(repository.summary(any(), any())).thenReturn(null);

        service.summary(null, null);

        verify(repository).summary(LocalDate.now().minusDays(30), LocalDate.now());
    }
}
