package com.srm.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

class SwaggerLocationRewriteFilterTest {

    private final SwaggerLocationRewriteFilter filter = new SwaggerLocationRewriteFilter();

    /** Chain que completa a resposta com 302 + Location, disparando o beforeCommit do filtro. */
    private static WebFilterChain redirectTo(String location) {
        return exchange -> {
            exchange.getResponse().setStatusCode(HttpStatus.FOUND);
            exchange.getResponse().getHeaders().set("Location", location);
            return exchange.getResponse().setComplete();
        };
    }

    @Test
    void rewritesLocationKeepingServicePrefix() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/swagger/credit-service/swagger-ui.html"));

        filter.filter(exchange, redirectTo("/swagger-ui/index.html")).block();

        assertThat(exchange.getResponse().getHeaders().getFirst("Location"))
                .isEqualTo("/swagger/credit-service/swagger-ui/index.html");
    }

    @Test
    void ignoresRequestsOutsideSwaggerPrefix() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/receivables"));

        filter.filter(exchange, redirectTo("/swagger-ui/index.html")).block();

        assertThat(exchange.getResponse().getHeaders().getFirst("Location"))
                .isEqualTo("/swagger-ui/index.html");
    }

    @Test
    void leavesNonSwaggerUiLocationsUntouched() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/swagger/credit-service/v3/api-docs"));

        filter.filter(exchange, redirectTo("/other/page")).block();

        assertThat(exchange.getResponse().getHeaders().getFirst("Location"))
                .isEqualTo("/other/page");
    }
}
