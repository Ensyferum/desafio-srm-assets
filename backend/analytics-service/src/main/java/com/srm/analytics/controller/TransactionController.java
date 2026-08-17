package com.srm.analytics.controller;

import com.srm.analytics.dto.PageResponse;
import com.srm.analytics.dto.TransactionSummary;
import com.srm.analytics.repo.SettlementAnalyticsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Extrato de liquidações (RF05). */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(
        name = "Extrato de Liquidações",
        description = "Consulta analítica com paginação server-side (RF05)")
public class TransactionController {

    private final SettlementAnalyticsRepository analyticsRepository;

    public TransactionController(SettlementAnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @GetMapping
    @Operation(
            summary = "Extrato de liquidações com filtros",
            description =
                    "Filtros: startDate, endDate, cedenteDocument, currency. Paginação: page, size, sort.")
    public PageResponse<TransactionSummary> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @RequestParam(required = false) String cedenteDocument,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "settledAt,desc") String sort) {

        Sort order = parseSort(sort);
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(size, 100), order);
        return analyticsRepository.findSettlements(
                startDate, endDate, cedenteDocument, currency, pageable);
    }

    private Sort parseSort(String sort) {
        try {
            String[] parts = sort.split(",");
            String property = parts[0].trim();
            Sort.Direction direction =
                    parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                            ? Sort.Direction.DESC
                            : Sort.Direction.ASC;
            // mapeia nomes amigáveis para colunas reais da projeção
            String column =
                    switch (property) {
                        case "settledAt", "settled_at" -> "settled_at";
                        case "presentValue", "present_value" -> "present_value";
                        case "faceValue", "face_value" -> "face_value";
                        default -> "settled_at";
                    };
            return Sort.by(direction, column);
        } catch (Exception ex) {
            return Sort.by(Sort.Direction.DESC, "settled_at");
        }
    }
}
