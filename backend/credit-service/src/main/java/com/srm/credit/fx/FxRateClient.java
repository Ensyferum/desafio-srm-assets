package com.srm.credit.fx;

import com.srm.common.correlation.CorrelationIdClientHttpRequestInterceptor;
import com.srm.common.error.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP do currency-service (RNF01) — uma tentativa por chamada. A resiliência (retry com
 * backoff, circuit breaker e fallback para a última taxa conhecida) é aplicada pelo {@link
 * FxConversionService} via Resilience4j; este cliente é intencionalmente enxuto e testável.
 */
@Component
public class FxRateClient {

    private static final Logger log = LoggerFactory.getLogger(FxRateClient.class);

    private final RestClient restClient;

    public FxRateClient(
            RestClient.Builder builder,
            CorrelationIdClientHttpRequestInterceptor correlationInterceptor,
            @Value("${app.fx.service-url:http://currency-service:8080}") String baseUrl) {
        RestClient.Builder prepared =
                builder.baseUrl(baseUrl).requestInterceptor(correlationInterceptor);
        this.restClient = configure(prepared).build();
    }

    /** Hook de configuração (testes sobrescrevem para injetar MockRestServiceServer). */
    protected RestClient.Builder configure(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofMillis(2000).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofMillis(2000).toMillis());
        return builder.requestFactory(requestFactory);
    }

    /**
     * Busca a taxa do par {@code from→to}. Se o par não existir, tenta o par invertido e retorna
     * {@code 1/rate}. Erros transitórios (5xx, rede) propagam para o retry/circuit breaker do
     * chamador.
     */
    public BigDecimal fetchRate(String from, String to, LocalDate date) {
        try {
            return fetchPair(from, to, date);
        } catch (HttpClientErrorException.NotFound notFound) {
            log.info("Par {}/{} não encontrado; tentando inversão de {}/{}", from, to, to, from);
            try {
                BigDecimal inverse = fetchPair(to, from, date);
                return BigDecimal.ONE.divide(inverse, 10, RoundingMode.HALF_EVEN);
            } catch (HttpClientErrorException.NotFound inverseNotFound) {
                throw new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Nenhuma taxa de câmbio encontrada para o par "
                                + from
                                + "→"
                                + to
                                + " na data "
                                + date);
            }
        }
    }

    private BigDecimal fetchPair(String from, String to, LocalDate date) {
        ExchangeRateResponse response =
                restClient
                        .get()
                        .uri(
                                "/api/v1/exchange-rates?from={from}&to={to}&date={date}",
                                from,
                                to,
                                date)
                        .retrieve()
                        .body(ExchangeRateResponse.class);
        if (response == null) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY, "Resposta vazia do serviço de câmbio.");
        }
        return response.rate();
    }
}
