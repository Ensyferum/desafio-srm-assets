package com.srm.gateway.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reescreve o header {@code Location} do redirect do Swagger UI (RF04).
 *
 * <p>O springdoc redireciona {@code /swagger-ui.html} → {@code /swagger-ui/index.html} com um
 * caminho absoluto, o que quebraria atrás do gateway (prefixo {@code /swagger/{serviço}}). Este
 * filtro reescreve o {@code Location} para manter o prefixo do serviço na URL pública.
 */
@Component
public class SwaggerLocationRewriteFilter implements WebFilter {

    private static final Pattern SWAGGER_PREFIX = Pattern.compile("^/swagger/(?<service>[^/]+)/");
    private static final Pattern SWAGGER_UI_REDIRECT = Pattern.compile("^/swagger-ui/");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Matcher matcher = SWAGGER_PREFIX.matcher(exchange.getRequest().getPath().value());
        if (!matcher.find()) {
            return chain.filter(exchange);
        }
        String service = matcher.group("service");
        exchange.getResponse()
                .beforeCommit(
                        () -> {
                            String location =
                                    exchange.getResponse().getHeaders().getFirst("Location");
                            if (location != null && SWAGGER_UI_REDIRECT.matcher(location).find()) {
                                exchange.getResponse()
                                        .getHeaders()
                                        .set("Location", "/swagger/" + service + location);
                            }
                            return Mono.empty();
                        });
        return chain.filter(exchange);
    }
}
