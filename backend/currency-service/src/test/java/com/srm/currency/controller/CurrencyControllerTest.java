package com.srm.currency.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.currency.dto.CurrencyResponse;
import com.srm.currency.service.CurrencyService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CurrencyController.class)
class CurrencyControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CurrencyService currencyService;

    @Test
    void listsActiveCurrencies() throws Exception {
        when(currencyService.listActive())
                .thenReturn(
                        List.of(
                                new CurrencyResponse(
                                        UUID.randomUUID(), "BRL", "Real Brasileiro", "R$"),
                                new CurrencyResponse(
                                        UUID.randomUUID(), "USD", "Dólar Americano", "US$")));

        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("BRL"))
                .andExpect(jsonPath("$[1].symbol").value("US$"));
    }
}
