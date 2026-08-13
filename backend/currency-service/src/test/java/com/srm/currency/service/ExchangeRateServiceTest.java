package com.srm.currency.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.common.error.BusinessException;
import com.srm.common.event.FxUpdatedEvent;
import com.srm.currency.domain.Currency;
import com.srm.currency.domain.ExchangeRate;
import com.srm.currency.domain.ExchangeRateRepository;
import com.srm.currency.dto.ExchangeRateRequest;
import com.srm.currency.dto.ExchangeRateResponse;
import com.srm.currency.fx.FxRateCache;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ExchangeRateServiceTest {

    private ExchangeRateRepository repository;
    private CurrencyService currencyService;
    private FxRateCache cache;
    private ApplicationEventPublisher eventPublisher;
    private ExchangeRateService service;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 12);

    @BeforeEach
    void setUp() {
        repository = mock(ExchangeRateRepository.class);
        currencyService = mock(CurrencyService.class);
        cache = mock(FxRateCache.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new ExchangeRateService(repository, currencyService, cache, eventPublisher);
        when(currencyService.requireCurrency("USD")).thenReturn(new Currency("USD", "Dólar", "$"));
        when(currencyService.requireCurrency("BRL")).thenReturn(new Currency("BRL", "Real", "R$"));
    }

    private ExchangeRateRequest request(BigDecimal rate) {
        return new ExchangeRateRequest("USD", "BRL", rate, DATE);
    }

    @Test
    void createsNewRateWhenPairDoesNotExist() {
        when(repository.findByFromCurrencyAndToCurrencyAndEffectiveDate("USD", "BRL", DATE))
                .thenReturn(Optional.empty());
        ExchangeRate saved =
                new ExchangeRate("USD", "BRL", new BigDecimal("5.4500000000"), DATE, "manager");
        when(repository.save(any(ExchangeRate.class))).thenReturn(saved);

        ExchangeRateResponse response =
                service.createOrUpdate(request(new BigDecimal("5.4500000000")), "manager");

        assertThat(response.fromCurrency()).isEqualTo("USD");
        assertThat(response.rate()).isEqualByComparingTo("5.45");
        verify(repository).save(any(ExchangeRate.class));
        verify(cache).evict("USD", "BRL", DATE);
        verify(eventPublisher).publishEvent(any(FxUpdatedEvent.class));
    }

    @Test
    void updatesExistingRateKeepingVigency() {
        ExchangeRate existing =
                new ExchangeRate("USD", "BRL", new BigDecimal("5.1000000000"), DATE, "manager");
        when(repository.findByFromCurrencyAndToCurrencyAndEffectiveDate("USD", "BRL", DATE))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(ExchangeRate.class))).thenAnswer(inv -> inv.getArgument(0));

        ExchangeRateResponse response =
                service.createOrUpdate(request(new BigDecimal("5.4523")), "manager");

        assertThat(response.rate()).isEqualByComparingTo("5.4523");
        verify(repository).save(existing);
    }

    @Test
    void rejectsSameCurrencyPair() {
        assertThatThrownBy(
                        () ->
                                service.createOrUpdate(
                                        new ExchangeRateRequest(
                                                "USD", "USD", new BigDecimal("1"), DATE),
                                        "m"))
                .isInstanceOf(BusinessException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void findRateReturnsCachedValueWhenPresent() {
        ExchangeRateResponse cached =
                new ExchangeRateResponse("USD", "BRL", new BigDecimal("5.4523"), DATE);
        when(cache.get("USD", "BRL", DATE)).thenReturn(Optional.of(cached));

        ExchangeRateResponse response = service.findRate("USD", "BRL", DATE);

        assertThat(response.rate()).isEqualByComparingTo("5.4523");
        verify(repository, never())
                .findByFromCurrencyAndToCurrencyAndEffectiveDate(any(), any(), any());
    }

    @Test
    void findRateReadsFromDatabaseAndPopulatesCacheOnMiss() {
        when(cache.get("USD", "BRL", DATE)).thenReturn(Optional.empty());
        ExchangeRate rate =
                new ExchangeRate("USD", "BRL", new BigDecimal("5.4523000000"), DATE, "seed");
        when(repository.findByFromCurrencyAndToCurrencyAndEffectiveDate("USD", "BRL", DATE))
                .thenReturn(Optional.of(rate));

        ExchangeRateResponse response = service.findRate("USD", "BRL", DATE);

        assertThat(response.rate()).isEqualByComparingTo("5.4523");
        verify(cache).put(eq("USD"), eq("BRL"), eq(DATE), any(ExchangeRateResponse.class));
    }

    @Test
    void findRateFallsBackToLatestRateBeforeDate() {
        when(cache.get("USD", "BRL", DATE)).thenReturn(Optional.empty());
        when(repository.findByFromCurrencyAndToCurrencyAndEffectiveDate("USD", "BRL", DATE))
                .thenReturn(Optional.empty());
        ExchangeRate older =
                new ExchangeRate(
                        "USD", "BRL", new BigDecimal("5.1000000000"), DATE.minusDays(5), "seed");
        when(repository.findByFromCurrencyAndToCurrencyOrderByEffectiveDateDesc("USD", "BRL"))
                .thenReturn(List.of(older));

        ExchangeRateResponse response = service.findRate("USD", "BRL", DATE);

        assertThat(response.effectiveDate()).isEqualTo(DATE.minusDays(5));
    }

    @Test
    void findRateThrowsNotFoundWhenNoRateAvailable() {
        when(cache.get("USD", "BRL", DATE)).thenReturn(Optional.empty());
        when(repository.findByFromCurrencyAndToCurrencyAndEffectiveDate("USD", "BRL", DATE))
                .thenReturn(Optional.empty());
        when(repository.findByFromCurrencyAndToCurrencyOrderByEffectiveDateDesc("USD", "BRL"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.findRate("USD", "BRL", DATE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não encontrada");
    }
}
