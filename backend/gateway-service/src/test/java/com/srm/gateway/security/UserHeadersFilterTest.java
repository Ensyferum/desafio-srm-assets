package com.srm.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

class UserHeadersFilterTest {

    private final UserHeadersFilter filter = new UserHeadersFilter();

    private Authentication authentication(String username, String... roles) {
        List<SimpleGrantedAuthority> authorities =
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }

    @Test
    void injectsUsernameAndRolesOnForward() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/transactions"));
        AtomicReference<String> username = new AtomicReference<>();
        AtomicReference<String> roles = new AtomicReference<>();

        filter.forward(
                        exchange,
                        e -> {
                            username.set(e.getRequest().getHeaders().getFirst("X-Username"));
                            roles.set(e.getRequest().getHeaders().getFirst("X-Roles"));
                            return Mono.empty();
                        },
                        authentication("operator1", "ROLE_OPERATOR"))
                .block();

        assertThat(username.get()).isEqualTo("operator1");
        assertThat(roles.get()).isEqualTo("OPERATOR");
    }

    @Test
    void keepsMultipleRolesCommaSeparated() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/receivables"));
        AtomicReference<String> roles = new AtomicReference<>();

        filter.forward(
                        exchange,
                        e -> {
                            roles.set(e.getRequest().getHeaders().getFirst("X-Roles"));
                            return Mono.empty();
                        },
                        authentication("manager1", "ROLE_MANAGER", "ROLE_OPERATOR"))
                .block();

        assertThat(roles.get()).isEqualTo("MANAGER,OPERATOR");
    }

    @Test
    void forwardSkipsHeadersWhenAuthenticationNotPresent() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/auth/login"));
        AtomicReference<String> username = new AtomicReference<>("not-set");

        filter.forward(
                        exchange,
                        e -> {
                            username.set(e.getRequest().getHeaders().getFirst("X-Username"));
                            return Mono.empty();
                        },
                        null)
                .block();

        assertThat(username.get()).isNull();
    }

    @Test
    void passesThroughWhenNotAuthenticated() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/auth/login"));
        AtomicReference<String> username = new AtomicReference<>("not-set");

        WebFilter bareFilter = new UserHeadersFilter();
        bareFilter
                .filter(
                        exchange,
                        e -> {
                            username.set(e.getRequest().getHeaders().getFirst("X-Username"));
                            return Mono.empty();
                        })
                .block();

        assertThat(username.get()).isNull();
    }
}
