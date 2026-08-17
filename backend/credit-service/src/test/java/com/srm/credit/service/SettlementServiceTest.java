package com.srm.credit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.common.error.BusinessException;
import com.srm.common.event.SettlementEvent;
import com.srm.credit.config.SettlementMetrics;
import com.srm.credit.domain.Receivable;
import com.srm.credit.domain.ReceivableRepository;
import com.srm.credit.domain.ReceivableStatus;
import com.srm.credit.domain.ReceivableType;
import com.srm.credit.domain.Transaction;
import com.srm.credit.domain.TransactionRepository;
import com.srm.credit.dto.SettleRequest;
import com.srm.credit.dto.SettleResponse;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;

class SettlementServiceTest {

    private ReceivableRepository receivableRepository;
    private TransactionRepository transactionRepository;
    private PricingCalculator pricingCalculator;
    private SettlementMetrics metrics;
    private ApplicationEventPublisher eventPublisher;
    private SettlementService service;

    private static final UUID RECEIVABLE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        receivableRepository = mock(ReceivableRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        metrics = mock(SettlementMetrics.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        FxConversionService fx = mock(FxConversionService.class);
        when(fx.getRate(any(), any(), any())).thenReturn(new BigDecimal("5.4523"));
        PricingStrategyRegistry registry =
                new PricingStrategyRegistry(List.of(new StandardPricingStrategy()));
        pricingCalculator = new PricingCalculator(registry, fx);
        service =
                new SettlementService(
                        receivableRepository,
                        transactionRepository,
                        pricingCalculator,
                        metrics,
                        eventPublisher);
    }

    private Receivable pendingReceivable() {
        ReceivableType type =
                new ReceivableType("Duplicata Mercantil", new BigDecimal("0.015"), "Título");
        return new Receivable(
                "11222333000181",
                type,
                new BigDecimal("100000.00"),
                LocalDate.now().plusDays(90),
                "BRL");
    }

    @Test
    void settlesPendingReceivableInSameCurrency() {
        Receivable receivable = pendingReceivable();
        when(receivableRepository.findById(RECEIVABLE_ID)).thenReturn(Optional.of(receivable));
        when(receivableRepository.saveAndFlush(any())).thenReturn(receivable);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SettleResponse response =
                service.settle(RECEIVABLE_ID, new SettleRequest("BRL"), "operator1");

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.settlementCurrency()).isEqualTo("BRL");
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.SETTLED);
        verify(receivableRepository).saveAndFlush(receivable);
        verify(transactionRepository).save(any(Transaction.class));
        verify(eventPublisher).publishEvent(any(SettlementEvent.class));
        verify(metrics).countSettlement("BRL");
    }

    @Test
    void settlesCrossCurrencyReceivableWithFxConversion() {
        Receivable receivable = pendingReceivable();
        when(receivableRepository.findById(RECEIVABLE_ID)).thenReturn(Optional.of(receivable));
        when(receivableRepository.saveAndFlush(any())).thenReturn(receivable);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SettleResponse response =
                service.settle(RECEIVABLE_ID, new SettleRequest("USD"), "operator1");

        // sem taxa cadastrada → o FxRateClient lançaria; aqui esperamos resposta com moeda USD
        assertThat(response.settlementCurrency()).isEqualTo("USD");
    }

    @Test
    void rejectsUnknownReceivable() {
        when(receivableRepository.findById(RECEIVABLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.settle(RECEIVABLE_ID, new SettleRequest("BRL"), "op"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsNonPendingReceivable() {
        Receivable receivable = pendingReceivable();
        receivable.markSettled();
        when(receivableRepository.findById(RECEIVABLE_ID)).thenReturn(Optional.of(receivable));

        assertThatThrownBy(() -> service.settle(RECEIVABLE_ID, new SettleRequest("BRL"), "op"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void propagatesOptimisticLockingConflict() {
        Receivable receivable = pendingReceivable();
        when(receivableRepository.findById(RECEIVABLE_ID)).thenReturn(Optional.of(receivable));
        doThrow(new OptimisticLockingFailureException("version conflict"))
                .when(receivableRepository)
                .saveAndFlush(any());

        assertThatThrownBy(() -> service.settle(RECEIVABLE_ID, new SettleRequest("BRL"), "op"))
                .isInstanceOf(OptimisticLockingFailureException.class);
        verify(transactionRepository, never()).save(any());
    }
}
