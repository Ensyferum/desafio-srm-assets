package com.srm.credit.service;

import com.srm.common.error.BusinessException;
import com.srm.credit.config.SettlementMetrics;
import com.srm.credit.domain.ReceivableType;
import com.srm.credit.domain.ReceivableTypeRepository;
import com.srm.credit.dto.PriceSimulationRequest;
import com.srm.credit.dto.PriceSimulationResponse;
import com.srm.credit.pricing.PricingCalculator;
import com.srm.credit.pricing.PricingResult;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Simulação em tempo real do valor presente (RF02). */
@Service
public class PricingService {

    private final PricingCalculator pricingCalculator;
    private final ReceivableTypeRepository receivableTypeRepository;
    private final SettlementMetrics metrics;

    public PricingService(
            PricingCalculator pricingCalculator,
            ReceivableTypeRepository receivableTypeRepository,
            SettlementMetrics metrics) {
        this.pricingCalculator = pricingCalculator;
        this.receivableTypeRepository = receivableTypeRepository;
        this.metrics = metrics;
    }

    public PriceSimulationResponse simulate(PriceSimulationRequest request) {
        ReceivableType type =
                receivableTypeRepository
                        .findById(request.receivableTypeId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Tipo de recebível não encontrado."));

        PricingResult result =
                pricingCalculator.calculate(
                        type,
                        request.faceValue(),
                        request.dueDate(),
                        request.currency(),
                        request.settlementCurrency(),
                        request.baseRate(),
                        LocalDate.now());

        metrics.countSimulation();
        return PriceSimulationResponse.from(result);
    }
}
