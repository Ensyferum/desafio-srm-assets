package com.srm.currency.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.common.error.GlobalExceptionHandler;
import com.srm.currency.dto.ExchangeRateResponse;
import com.srm.currency.service.ExchangeRateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExchangeRateController.class)
@Import(GlobalExceptionHandler.class)
class ExchangeRateControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ExchangeRateService exchangeRateService;

    @Test
    void createsExchangeRateWith201() throws Exception {
        when(exchangeRateService.createOrUpdate(any(), eq("manager")))
                .thenReturn(
                        new ExchangeRateResponse(
                                "USD", "BRL", new BigDecimal("5.4523"), LocalDate.of(2026, 8, 12)));

        mockMvc.perform(
                        post("/api/v1/exchange-rates")
                                .header("X-Username", "manager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "fromCurrency": "USD",
                                          "toCurrency": "BRL",
                                          "rate": 5.4523,
                                          "effectiveDate": "2026-08-12"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromCurrency").value("USD"))
                .andExpect(jsonPath("$.rate").value(5.4523));
    }

    @Test
    void rejectsInvalidPayloadWith400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/exchange-rates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "fromCurrency": "USD",
                                          "toCurrency": "BRL",
                                          "rate": -1,
                                          "effectiveDate": "2026-08-12"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void queriesRateByPairAndDate() throws Exception {
        when(exchangeRateService.findRate(eq("USD"), eq("BRL"), any()))
                .thenReturn(
                        new ExchangeRateResponse(
                                "USD", "BRL", new BigDecimal("5.4523"), LocalDate.of(2026, 8, 12)));

        mockMvc.perform(
                        get("/api/v1/exchange-rates")
                                .param("from", "USD")
                                .param("to", "BRL")
                                .param("date", "2026-08-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toCurrency").value("BRL"));
    }
}
