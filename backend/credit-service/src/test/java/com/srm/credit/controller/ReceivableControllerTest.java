package com.srm.credit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.common.error.GlobalExceptionHandler;
import com.srm.credit.dto.PageResponse;
import com.srm.credit.dto.PriceSimulationResponse;
import com.srm.credit.dto.ReceivableResponse;
import com.srm.credit.dto.SettleResponse;
import com.srm.credit.service.PricingService;
import com.srm.credit.service.ReceivableService;
import com.srm.credit.service.SettlementService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReceivableController.class)
@Import(GlobalExceptionHandler.class)
class ReceivableControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PricingService pricingService;
    @MockitoBean private ReceivableService receivableService;
    @MockitoBean private SettlementService settlementService;

    @Test
    void simulatesPricing() throws Exception {
        when(pricingService.simulate(any()))
                .thenReturn(
                        new PriceSimulationResponse(
                                new BigDecimal("100000.00"),
                                new BigDecimal("94232.28"),
                                new BigDecimal("5767.72"),
                                new BigDecimal("0.015"),
                                new BigDecimal("3.000000"),
                                new BigDecimal("0.005"),
                                new BigDecimal("5.4523"),
                                new BigDecimal("17283.45"),
                                "BRL",
                                "USD",
                                "Duplicata Mercantil"));

        mockMvc.perform(
                        post("/api/v1/receivables/price")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "faceValue": 100000.00,
                                          "dueDate": "2026-11-12",
                                          "receivableTypeId": "%s",
                                          "currency": "BRL",
                                          "settlementCurrency": "USD",
                                          "baseRate": 0.005
                                        }
                                        """
                                                .formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presentValue").value(94232.28))
                .andExpect(jsonPath("$.presentValueInSettlementCurrency").value(17283.45));
    }

    @Test
    void settlesReceivable() throws Exception {
        when(settlementService.settle(
                        eq(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                        any(),
                        eq("operator")))
                .thenReturn(
                        new SettleResponse(
                                UUID.randomUUID(),
                                "COMPLETED",
                                new BigDecimal("94232.28"),
                                new BigDecimal("5767.72"),
                                "USD",
                                new BigDecimal("5.4523"),
                                new BigDecimal("17283.45"),
                                Instant.now()));

        mockMvc.perform(
                        post("/api/v1/receivables/11111111-1111-1111-1111-111111111111/settle")
                                .header("X-Username", "operator")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"settlementCurrency\":\"USD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.settlementCurrency").value("USD"));
    }

    @Test
    void listsReceivables() throws Exception {
        ReceivableResponse receivable =
                new ReceivableResponse(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        "Duplicata Mercantil",
                        new BigDecimal("10000.00"),
                        LocalDate.of(2026, 11, 12),
                        "BRL",
                        "PENDING",
                        0L);
        when(receivableService.list(eq("PENDING"), eq("BRL"), isNull(), any(Pageable.class)))
                .thenReturn(PageResponse.of(List.of(receivable), PageRequest.of(0, 20), 1));

        mockMvc.perform(
                        get("/api/v1/receivables")
                                .param("status", "PENDING")
                                .param("currency", "BRL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].currency").value("BRL"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void rejectsInvalidSettlementPayload() throws Exception {
        mockMvc.perform(
                        post("/api/v1/receivables/11111111-1111-1111-1111-111111111111/settle")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"settlementCurrency\":\"EUR\"}"))
                .andExpect(status().isBadRequest());
    }
}
