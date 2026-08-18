package com.srm.analytics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.analytics.dto.AnalyticsSummaryResponse;
import com.srm.analytics.dto.CedenteDistribution;
import com.srm.analytics.dto.TimeSeriesPoint;
import com.srm.analytics.service.AnalyticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AnalyticsService analyticsService;

    @Test
    void returnsSummaryWithExplicitDates() throws Exception {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        when(analyticsService.summary(start, end))
                .thenReturn(
                        new AnalyticsSummaryResponse(
                                3,
                                new BigDecimal("150000.00"),
                                new BigDecimal("9000.00"),
                                Map.of("BRL", new BigDecimal("150000.00")),
                                start,
                                end));

        mockMvc.perform(
                        get("/api/v1/analytics/summary")
                                .param("startDate", "2026-08-01")
                                .param("endDate", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(3))
                .andExpect(jsonPath("$.totalPresentValue").value(150000.00))
                .andExpect(jsonPath("$.presentValueByCurrency.BRL").value(150000.00));
    }

    @Test
    void toleratesMissingDateParams() throws Exception {
        when(analyticsService.summary(any(), any()))
                .thenReturn(
                        new AnalyticsSummaryResponse(
                                0,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                Map.of(),
                                LocalDate.now().minusDays(30),
                                LocalDate.now()));

        mockMvc.perform(get("/api/v1/analytics/summary")).andExpect(status().isOk());
    }

    @Test
    void returnsTimeSeriesWithExplicitDates() throws Exception {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        when(analyticsService.timeSeries(start, end))
                .thenReturn(
                        List.of(
                                new TimeSeriesPoint(start, "BRL", 2, new BigDecimal("100.00")),
                                new TimeSeriesPoint(start, "USD", 1, new BigDecimal("500.00"))));

        mockMvc.perform(
                        get("/api/v1/analytics/timeseries")
                                .param("startDate", "2026-08-01")
                                .param("endDate", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currency").value("BRL"))
                .andExpect(jsonPath("$[0].presentValue").value(100.00))
                .andExpect(jsonPath("$[1].currency").value("USD"));
    }

    @Test
    void returnsCedenteDistribution() throws Exception {
        when(analyticsService.distributionByCedente(any(), any()))
                .thenReturn(
                        List.of(
                                new CedenteDistribution(
                                        "11222333000181", 3, new BigDecimal("150000.00"))));

        mockMvc.perform(get("/api/v1/analytics/by-cedente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cedenteDocument").value("11222333000181"))
                .andExpect(jsonPath("$[0].transactions").value(3))
                .andExpect(jsonPath("$[0].presentValue").value(150000.00));
    }
}
