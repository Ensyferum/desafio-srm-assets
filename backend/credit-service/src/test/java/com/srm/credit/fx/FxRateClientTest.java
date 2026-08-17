package com.srm.credit.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.srm.common.correlation.CorrelationIdClientHttpRequestInterceptor;
import com.srm.common.error.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FxRateClientTest {

    private MockRestServiceServer server;
    private FxRateClient client;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 12);

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client =
                new TestFxRateClient(
                        builder,
                        new CorrelationIdClientHttpRequestInterceptor(),
                        "http://currency-service:8080");
    }

    /** Mantém a request factory do MockRestServiceServer (sem timeout real). */
    private static class TestFxRateClient extends FxRateClient {
        TestFxRateClient(
                RestClient.Builder builder,
                CorrelationIdClientHttpRequestInterceptor interceptor,
                String baseUrl) {
            super(builder, interceptor, baseUrl);
        }

        @Override
        protected RestClient.Builder configure(RestClient.Builder builder) {
            return builder;
        }
    }

    @Test
    void fetchesExactPairRate() {
        server.expect(
                        requestTo(
                                "http://currency-service:8080/api/v1/exchange-rates?from=USD&to=BRL&date=2026-08-12"))
                .andRespond(
                        withSuccess(
                                """
                                {"fromCurrency":"USD","toCurrency":"BRL","rate":5.4523,"effectiveDate":"2026-08-12"}
                                """,
                                MediaType.APPLICATION_JSON));

        BigDecimal rate = client.fetchRate("USD", "BRL", DATE);

        assertThat(rate).isEqualByComparingTo("5.4523");
        server.verify();
    }

    @Test
    void fallsBackToInvertedPairWhenNotFound() {
        server.expect(
                        requestTo(
                                "http://currency-service:8080/api/v1/exchange-rates?from=BRL&to=USD&date=2026-08-12"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(
                        requestTo(
                                "http://currency-service:8080/api/v1/exchange-rates?from=USD&to=BRL&date=2026-08-12"))
                .andRespond(
                        withSuccess(
                                """
                                {"fromCurrency":"USD","toCurrency":"BRL","rate":5.45,"effectiveDate":"2026-08-12"}
                                """,
                                MediaType.APPLICATION_JSON));

        BigDecimal rate = client.fetchRate("BRL", "USD", DATE);

        assertThat(rate).isEqualByComparingTo("0.1834862385");
        server.verify();
    }

    @Test
    void throwsNotFoundWhenBothPairsMissing() {
        server.expect(
                        requestTo(
                                "http://currency-service:8080/api/v1/exchange-rates?from=BRL&to=USD&date=2026-08-12"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(
                        requestTo(
                                "http://currency-service:8080/api/v1/exchange-rates?from=USD&to=BRL&date=2026-08-12"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.fetchRate("BRL", "USD", DATE))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        server.verify();
    }

    @Test
    void propagatesTransientErrorsToResilienceLayer() {
        server.expect(
                        requestTo(
                                "http://currency-service:8080/api/v1/exchange-rates?from=USD&to=BRL&date=2026-08-12"))
                .andRespond(
                        org.springframework.test.web.client.response.MockRestResponseCreators
                                .withServerError());

        // Erro 5xx propaga (RestClientException) — o retry/circuit breaker vive no
        // FxConversionService
        assertThatThrownBy(() -> client.fetchRate("USD", "BRL", DATE))
                .isInstanceOf(org.springframework.web.client.RestClientException.class);
        server.verify();
    }
}
