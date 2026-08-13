package com.srm.credit.controller;

import com.srm.credit.domain.ReceivableTypeRepository;
import com.srm.credit.dto.ReceivableTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de tipos de recebíveis. */
@RestController
@RequestMapping("/api/v1/receivable-types")
@Tag(name = "Tipos de Recebíveis", description = "Tipos com spread mensal configurável (RF02)")
public class ReceivableTypeController {

    private final ReceivableTypeRepository receivableTypeRepository;

    public ReceivableTypeController(ReceivableTypeRepository receivableTypeRepository) {
        this.receivableTypeRepository = receivableTypeRepository;
    }

    @GetMapping
    @Operation(summary = "Lista tipos de recebíveis com seus spreads")
    public List<ReceivableTypeResponse> list() {
        return receivableTypeRepository.findAllByOrderByName().stream()
                .map(ReceivableTypeResponse::from)
                .toList();
    }
}
