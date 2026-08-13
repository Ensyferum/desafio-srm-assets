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
import org.springframework.web.client.RestClientException;

/**
 * Cliente resiliente do currency-service (RNF01): retry com backoff simples em falhas transitórias
 * e fallback para o par de moedas invertido.
 */
@Component
public class FxRateClient {

    private static final Logger log = LoggerFactory.getLogger(FxRateClient.class);

    private final RestClient restClient;
    private final int maxAttempts;

    public FxRateClient(
            RestClient.Builder builder,
            CorrelationIdClientHttpRequestInterceptor correlationInterceptor,
            @Value("${app.fx.service-url:http://currency-service:8080}") String baseUrl,
            @Value("${app.fx.max-attempts:3}") int maxAttempts) {
        this.maxAttempts = maxAttempts;
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
     * {@code 1/rate}. Falhas transitórias usam retry.
     */
    public BigDecimal fetchRate(String from, String to, LocalDate date) {
        try {
            return fetchPair(from, to, date);
        } catch (HttpClientErrorException.NotFound notFound) {
            log.info("Par {}/{} não encontrado; tentando inversão de {}/{}", from, to, to, from);
            BigDecimal inverse = fetchPair(to, from, date);
            return BigDecimal.ONE.divide(inverse, 10, RoundingMode.HALF_EVEN);
        }
    }

    private BigDecimal fetchPair(String from, String to, LocalDate date) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
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
            } catch (HttpClientErrorException.NotFound notFound) {
                throw notFound;
            } catch (RestClientException ex) {
                if (attempt == maxAttempts) {
                    log.error(
                            "Falha ao consultar taxa {}/{} após {} tentativas: {}",
                            from,
                            to,
                            maxAttempts,
                            ex.getMessage());
                    throw new BusinessException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Serviço de câmbio indisponível no momento. Tente novamente.");
                }
                sleepBackoff(attempt);
            }
        }
        throw new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE, "Serviço de câmbio indisponível.");
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(200L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Operação interrompida.");
        }
    }
}
