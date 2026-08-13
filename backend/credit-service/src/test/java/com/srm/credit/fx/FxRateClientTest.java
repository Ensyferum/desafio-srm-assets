package com.srm.credit.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
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
                        "http://currency-service:8080",
                        3);
    }

    /** Mantém a request factory do MockRestServiceServer (sem timeout real). */
    private static class TestFxRateClient extends FxRateClient {
        TestFxRateClient(
                RestClient.Builder builder,
                CorrelationIdClientHttpRequestInterceptor interceptor,
                String baseUrl,
                int maxAttempts) {
            super(builder, interceptor, baseUrl, maxAttempts);
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
    void retriesTransientErrorsThenFailsWith503() {
        for (int i = 0; i < 3; i++) {
            server.expect(
                            requestTo(
                                    "http://currency-service:8080/api/v1/exchange-rates?from=USD&to=BRL&date=2026-08-12"))
                    .andRespond(withServerError());
        }

        assertThatThrownBy(() -> client.fetchRate("USD", "BRL", DATE))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        server.verify();
    }
}
