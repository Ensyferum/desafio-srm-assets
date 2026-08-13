package com.srm.currency.controller;

import com.srm.currency.dto.ExchangeRateRequest;
import com.srm.currency.dto.ExchangeRateResponse;
import com.srm.currency.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de taxas de câmbio (RF01). */
@RestController
@RequestMapping("/api/v1/exchange-rates")
@Tag(name = "Taxas de Câmbio", description = "Gestão de taxas de câmbio BRL/USD (RF01)")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria/atualiza taxa de câmbio (requer MANAGER+)")
    public ExchangeRateResponse create(
            @Valid @RequestBody ExchangeRateRequest request,
            @RequestHeader(value = "X-Username", required = false) String createdBy) {
        return exchangeRateService.createOrUpdate(
                request, createdBy == null ? "anonymous" : createdBy);
    }

    @GetMapping
    @Operation(
            summary = "Consulta taxa de câmbio",
            description =
                    "Com from/to/date retorna a taxa vigente (exata ou a mais recente anterior). "
                            + "Sem par, retorna histórico.")
    public Object get(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate) {
        if (from != null || to != null) {
            return exchangeRateService.findRate(from, to, date);
        }
        return exchangeRateService.list(from, to, startDate, endDate);
    }
}
