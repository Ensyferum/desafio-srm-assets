package com.srm.gateway.filter;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Garante um {@code X-Correlation-Id} em toda a solicitação que atravessa o gateway: reutiliza o
 * header de entrada ou gera um UUID, propaga para o serviço de destino, devolve no header da
 * resposta e registra no MDC.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        if (correlationId.length() > 64) {
            correlationId = correlationId.substring(0, 64);
        }

        MDC.put(MDC_KEY, correlationId);
        ServerHttpRequest request =
                exchange.getRequest().mutate().header(HEADER, correlationId).build();
        exchange.getResponse().getHeaders().set(HEADER, correlationId);

        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signal -> MDC.remove(MDC_KEY));
    }
}
