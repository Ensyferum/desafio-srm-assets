package com.srm.credit.controller;

import com.srm.credit.dto.CreateReceivablesBatchRequest;
import com.srm.credit.dto.CreateReceivablesBatchResponse;
import com.srm.credit.dto.PageResponse;
import com.srm.credit.dto.PriceSimulationRequest;
import com.srm.credit.dto.PriceSimulationResponse;
import com.srm.credit.dto.ReceivableResponse;
import com.srm.credit.dto.SettleRequest;
import com.srm.credit.dto.SettleResponse;
import com.srm.credit.service.PricingService;
import com.srm.credit.service.ReceivableService;
import com.srm.credit.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de recebíveis e liquidação (RF02/RF03). */
@RestController
@RequestMapping("/api/v1/receivables")
@Tag(name = "Recebíveis", description = "Precificação, lote de recebíveis e liquidação")
public class ReceivableController {

    private final PricingService pricingService;
    private final ReceivableService receivableService;
    private final SettlementService settlementService;

    public ReceivableController(
            PricingService pricingService,
            ReceivableService receivableService,
            SettlementService settlementService) {
        this.pricingService = pricingService;
        this.receivableService = receivableService;
        this.settlementService = settlementService;
    }

    @PostMapping("/price")
    @Operation(summary = "Simula a precificação em tempo real (RF02)")
    public PriceSimulationResponse price(@Valid @RequestBody PriceSimulationRequest request) {
        return pricingService.simulate(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra lote de recebíveis")
    public CreateReceivablesBatchResponse createBatch(
            @Valid @RequestBody CreateReceivablesBatchRequest request) {
        return receivableService.createBatch(request);
    }

    @PostMapping("/{id}/settle")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Liquida um recebível (ACID + optimistic locking)")
    public SettleResponse settle(
            @PathVariable UUID id,
            @Valid @RequestBody SettleRequest request,
            @RequestHeader(value = "X-Username", required = false) String createdBy) {
        return settlementService.settle(id, request, createdBy);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca recebível por ID")
    public ReceivableResponse findById(@PathVariable UUID id) {
        return receivableService.findById(id);
    }

    @GetMapping
    @Operation(
            summary =
                    "Lista recebíveis com filtros (status, moeda, documento do cedente) e paginação")
    public PageResponse<ReceivableResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String cedenteDocument,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return receivableService.list(status, currency, cedenteDocument, pageable);
    }
}
