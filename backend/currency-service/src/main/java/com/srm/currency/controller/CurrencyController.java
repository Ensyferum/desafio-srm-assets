package com.srm.currency.controller;

import com.srm.currency.dto.CurrencyResponse;
import com.srm.currency.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de moedas. */
@RestController
@RequestMapping("/api/v1/currencies")
@Tag(name = "Moedas", description = "Moedas suportadas pela plataforma")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping
    @Operation(summary = "Lista moedas ativas")
    public List<CurrencyResponse> list() {
        return currencyService.listActive();
    }
}
