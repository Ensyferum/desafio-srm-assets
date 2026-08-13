package com.srm.credit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.common.error.BusinessException;
import com.srm.credit.config.SettlementMetrics;
import com.srm.credit.domain.ReceivableType;
import com.srm.credit.domain.ReceivableTypeRepository;
import com.srm.credit.dto.PriceSimulationRequest;
import com.srm.credit.dto.PriceSimulationResponse;
import com.srm.credit.fx.FxConversionService;
import com.srm.credit.pricing.PricingCalculator;
import com.srm.credit.pricing.PricingStrategyRegistry;
import com.srm.credit.pricing.StandardPricingStrategy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PricingServiceTest {

    private ReceivableTypeRepository typeRepository;
    private SettlementMetrics metrics;
    private PricingService service;

    private final UUID typeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        typeRepository = mock(ReceivableTypeRepository.class);
        metrics = mock(SettlementMetrics.class);
        FxConversionService fx = mock(FxConversionService.class);
        PricingStrategyRegistry registry =
                new PricingStrategyRegistry(List.of(new StandardPricingStrategy()));
        PricingCalculator calculator = new PricingCalculator(registry, fx);
        service = new PricingService(calculator, typeRepository, metrics);
        when(typeRepository.findById(typeId))
                .thenReturn(
                        Optional.of(
                                new ReceivableType(
                                        "Duplicata Mercantil", new BigDecimal("0.015"), "Título")));
    }

    @Test
    void simulatesPricingAndCountsMetric() {
        PriceSimulationResponse response =
                service.simulate(
                        new PriceSimulationRequest(
                                new BigDecimal("100000.00"),
                                LocalDate.now().plusDays(90),
                                typeId,
                                "BRL",
                                "BRL",
                                new BigDecimal("0.005")));

        // valor matematicamente correto de 100.000 / 1,061208 (a spec afirma 94232.28 por erro de
        // arredondamento)
        assertThat(response.presentValue()).isEqualByComparingTo("94232.23");
        assertThat(response.discountValue()).isEqualByComparingTo("5767.77");
        verify(metrics).countSimulation();
    }

    @Test
    void throws404WhenTypeNotFound() {
        when(typeRepository.findById(typeId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.simulate(
                                        new PriceSimulationRequest(
                                                new BigDecimal("1000.00"),
                                                LocalDate.now().plusDays(30),
                                                typeId,
                                                "BRL",
                                                "BRL",
                                                null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
