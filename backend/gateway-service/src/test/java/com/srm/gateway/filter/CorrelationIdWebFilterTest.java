package com.srm.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class CorrelationIdWebFilterTest {

    private final CorrelationIdWebFilter filter = new CorrelationIdWebFilter();

    @Test
    void generatesAndPropagatesCorrelationId() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/health"));
        AtomicReference<String> seenByChain = new AtomicReference<>();

        filter.filter(
                        exchange,
                        e -> {
                            seenByChain.set(
                                    e.getRequest()
                                            .getHeaders()
                                            .getFirst(CorrelationIdWebFilter.HEADER));
                            return Mono.empty();
                        })
                .block();

        assertThat(seenByChain.get()).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdWebFilter.HEADER))
                .isEqualTo(seenByChain.get());
    }

    @Test
    void reusesIncomingCorrelationId() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/health")
                                .header(CorrelationIdWebFilter.HEADER, "cid-incoming"));

        filter.filter(exchange, e -> Mono.empty()).block();

        assertThat(exchange.getRequest().getHeaders().getFirst(CorrelationIdWebFilter.HEADER))
                .isEqualTo("cid-incoming");
    }

    @Test
    void sanitizesOversizedIncomingHeader() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/health")
                                .header(CorrelationIdWebFilter.HEADER, "x".repeat(200)));
        AtomicReference<String> seenByChain = new AtomicReference<>();

        filter.filter(
                        exchange,
                        e -> {
                            seenByChain.set(
                                    e.getRequest()
                                            .getHeaders()
                                            .getFirst(CorrelationIdWebFilter.HEADER));
                            return Mono.empty();
                        })
                .block();

        assertThat(seenByChain.get()).hasSize(64);
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdWebFilter.HEADER))
                .hasSize(64);
    }

    @Test
    void clearsMdcAfterCompletion() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/health"));

        filter.filter(exchange, e -> Mono.empty()).block();

        assertThat(MDC.get(CorrelationIdWebFilter.MDC_KEY)).isNull();
    }
}
