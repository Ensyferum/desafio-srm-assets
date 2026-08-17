package com.srm.credit.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.srm.common.error.BusinessException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;

class FxConversionServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 12);

    private FxRateClient fxRateClient;
    private FxConversionService service;

    @BeforeEach
    void setUp() {
        fxRateClient = mock(FxRateClient.class);
        CircuitBreakerRegistry cbRegistry =
                CircuitBreakerRegistry.of(
                        CircuitBreakerConfig.custom()
                                .slidingWindowSize(4)
                                .failureRateThreshold(50)
                                .waitDurationInOpenState(Duration.ofMillis(100))
                                .build());
        RetryRegistry retryRegistry =
                RetryRegistry.of(
                        RetryConfig.custom()
                                .maxAttempts(3)
                                .waitDuration(Duration.ofMillis(10))
                                .retryExceptions(RestClientException.class)
                                .ignoreExceptions(BusinessException.class)
                                .build());
        service = new FxConversionService(fxRateClient, cbRegistry, retryRegistry);
    }

    @Test
    void returnsOneWhenSameCurrency() {
        assertThat(service.getRate("BRL", "BRL", DATE)).isEqualByComparingTo("1");
        assertThat(service.getRate("USD", "usd", DATE)).isEqualByComparingTo("1");
    }

    @Test
    void returnsRateAndCachesLastKnown() {
        when(fxRateClient.fetchRate("USD", "BRL", DATE)).thenReturn(new BigDecimal("5.4523"));

        assertThat(service.getRate("USD", "BRL", DATE)).isEqualByComparingTo("5.4523");
    }

    @Test
    void retriesTransientFailuresThenSucceeds() {
        // 1º e 2º falham (transitório), 3º funciona → retry cobre a falha pontual
        when(fxRateClient.fetchRate("USD", "BRL", DATE))
                .thenThrow(new RestClientException("timeout"))
                .thenThrow(new RestClientException("timeout"))
                .thenReturn(new BigDecimal("5.5"));

        assertThat(service.getRate("USD", "BRL", DATE)).isEqualByComparingTo("5.5");
    }

    @Test
    void fallsBackToLastKnownRateWhenServiceDown() {
        when(fxRateClient.fetchRate("USD", "BRL", DATE)).thenReturn(new BigDecimal("5.4523"));
        assertThat(service.getRate("USD", "BRL", DATE)).isEqualByComparingTo("5.4523");

        // serviço cai de vez → retry esgota → usa a última taxa conhecida (sem 5xx)
        when(fxRateClient.fetchRate("USD", "BRL", DATE))
                .thenThrow(new RestClientException("connection refused"));
        assertThat(service.getRate("USD", "BRL", DATE)).isEqualByComparingTo("5.4523");
    }

    @Test
    void throws503WhenNoLastKnownRate() {
        when(fxRateClient.fetchRate("USD", "BRL", DATE))
                .thenThrow(new RestClientException("connection refused"));

        assertThatThrownBy(() -> service.getRate("USD", "BRL", DATE))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void rethrowsBusinessErrorsWithoutFallback() {
        BusinessException missing =
                new BusinessException(HttpStatus.NOT_FOUND, "Par não encontrado");
        when(fxRateClient.fetchRate("USD", "BRL", DATE)).thenThrow(missing);

        // erro de negócio (ex.: par inexistente) NÃO usa retry/fallback — propaga
        assertThatThrownBy(() -> service.getRate("USD", "BRL", DATE))
                .isInstanceOf(BusinessException.class)
                .isSameAs(missing);
    }

    @Test
    void circuitBreakerOpensAndStopsCallingClient() {
        // 1ª chamada bem-sucedida grava a última taxa conhecida
        when(fxRateClient.fetchRate("USD", "BRL", DATE)).thenReturn(new BigDecimal("5.4523"));
        assertThat(service.getRate("USD", "BRL", DATE)).isEqualByComparingTo("5.4523");

        // A partir daqui o serviço falha sempre
        when(fxRateClient.fetchRate("USD", "BRL", DATE)).thenThrow(new RestClientException("down"));
        for (int i = 0; i < 4; i++) {
            assertThat(service.getRate("USD", "BRL", DATE)).isEqualByComparingTo("5.4523");
        }

        long callsBefore = countClientCalls();

        // Com o CB aberto, o fallback é direto: o client NÃO é mais chamado
        assertThat(service.getRate("USD", "BRL", DATE)).isEqualByComparingTo("5.4523");
        assertThat(countClientCalls()).isEqualTo(callsBefore);
    }

    private long countClientCalls() {
        return org.mockito.Mockito.mockingDetails(fxRateClient).getInvocations().stream()
                .filter(inv -> inv.getMethod().getName().equals("fetchRate"))
                .count();
    }
}
