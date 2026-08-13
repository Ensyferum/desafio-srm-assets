package com.srm.gateway.security;

import java.util.stream.Collectors;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Injeta os headers {@code X-Username} e {@code X-Roles} nas requisições encaminhadas aos serviços
 * de destino, a partir do JWT já autenticado.
 */
@Component
public class UserHeadersFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> forward(exchange, chain, authentication))
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)));
    }

    /** Injeta os headers do usuário autenticado na requisição encaminhada. */
    Mono<Void> forward(
            ServerWebExchange exchange, WebFilterChain chain, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return chain.filter(exchange);
        }
        String username = authentication.getName();
        String roles =
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(authority -> authority.replaceFirst("^ROLE_", ""))
                        .collect(Collectors.joining(","));
        ServerHttpRequest request =
                exchange.getRequest()
                        .mutate()
                        .header("X-Username", username)
                        .header("X-Roles", roles)
                        .build();
        return chain.filter(exchange.mutate().request(request).build());
    }
}
