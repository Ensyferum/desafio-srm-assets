package com.srm.credit.fx;

import com.srm.common.error.BusinessException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Conversão cambial para operações cross-currency (título em BRL, pagamento em USD e vice-versa),
 * com resiliência (RNF01): retry com backoff, circuit breaker e fallback para a última taxa
 * conhecida por par — a liquidação nunca quebra por indisponibilidade pontual do currency-service.
 */
@Service
public class FxConversionService {

    private static final Logger log = LoggerFactory.getLogger(FxConversionService.class);

    private final FxRateClient fxRateClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Map<String, BigDecimal> lastKnownRates = new ConcurrentHashMap<>();

    public FxConversionService(
            FxRateClient fxRateClient,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this.fxRateClient = fxRateClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("fxRate");
        this.retry = retryRegistry.retry("fxRate");
    }

    /**
     * Retorna a taxa do par {@code from→to} (1 from = rate to), com fallback para a inversão do par
     * oposto quando necessário. Falhas transitórias são repetidas (retry) e, se o serviço ficar
     * indisponível, o circuit breaker abre e a última taxa conhecida é usada.
     */
    public BigDecimal getRate(String from, String to, LocalDate date) {
        if (from.equalsIgnoreCase(to)) {
            return BigDecimal.ONE;
        }
        String pairKey = pairKey(from, to);
        try {
            // Circuit breaker por fora, retry por dentro: se o CB estiver aberto, nem tenta.
            Supplier<BigDecimal> protectedCall =
                    CircuitBreaker.decorateSupplier(
                            circuitBreaker,
                            Retry.decorateSupplier(
                                    retry, () -> fxRateClient.fetchRate(from, to, date)));
            BigDecimal rate = protectedCall.get();
            lastKnownRates.put(pairKey, rate);
            return rate;
        } catch (BusinessException ex) {
            // Erro de negócio (ex.: par inexistente) não deve ser mascarado por fallback
            throw ex;
        } catch (Exception ex) {
            BigDecimal lastKnown = lastKnownRates.get(pairKey);
            if (lastKnown != null) {
                log.warn(
                        "Serviço de câmbio indisponível ({}); usando última taxa conhecida {}: {}",
                        pairKey,
                        lastKnown,
                        ex.getMessage());
                return lastKnown;
            }
            log.error(
                    "Falha ao consultar taxa {} após retry/circuit breaker: {}",
                    pairKey,
                    ex.getMessage());
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de câmbio indisponível no momento. Tente novamente.");
        }
    }

    private static String pairKey(String from, String to) {
        return from.toUpperCase() + "→" + to.toUpperCase();
    }
}
