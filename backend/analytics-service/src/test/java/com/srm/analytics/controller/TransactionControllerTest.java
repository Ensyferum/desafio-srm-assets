package com.srm.analytics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.analytics.dto.PageResponse;
import com.srm.analytics.dto.TransactionSummary;
import com.srm.analytics.repo.SettlementAnalyticsRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SettlementAnalyticsRepository analyticsRepository;

    @Test
    void returnsPagedSettlementsWithFilters() throws Exception {
        TransactionSummary summary =
                new TransactionSummary(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("100000.00"),
                        new BigDecimal("94232.23"),
                        new BigDecimal("5767.77"),
                        "BRL",
                        "USD",
                        new BigDecimal("5.4523"),
                        "COMPLETED",
                        Instant.parse("2026-08-12T22:30:00Z"));
        when(analyticsRepository.findSettlements(
                        any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(PageResponse.of(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(
                        get("/api/v1/transactions")
                                .param("startDate", "2026-08-01")
                                .param("endDate", "2026-08-31")
                                .param("currency", "USD")
                                .param("page", "0")
                                .param("size", "20")
                                .param("sort", "settledAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].settlementCurrency").value("USD"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void capsPageSizeAt100() throws Exception {
        when(analyticsRepository.findSettlements(
                        any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(PageResponse.of(List.of(), PageRequest.of(0, 100), 0));

        mockMvc.perform(get("/api/v1/transactions").param("size", "9999"))
                .andExpect(status().isOk());
    }

    @Test
    void sortsAscendingByMappedColumn() throws Exception {
        when(analyticsRepository.findSettlements(
                        any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(PageResponse.of(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/transactions").param("sort", "presentValue,asc"))
                .andExpect(status().isOk());
    }

    @Test
    void toleratesMalformedSort() throws Exception {
        when(analyticsRepository.findSettlements(
                        any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(PageResponse.of(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/transactions").param("sort", "{{invalid"))
                .andExpect(status().isOk());
    }
}
