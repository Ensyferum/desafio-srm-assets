package com.srm.gateway.security.rbac;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

class DynamicRouteRoleAuthorizationManagerTest {

    @Test
    void permitAllRuleAllowsUnauthenticatedRequest() {
        TestRuleSource source =
                new TestRuleSource(
                        List.of(
                                new RouteRoleRule(
                                        "public",
                                        "GET",
                                        "/public/**",
                                        List.of(),
                                        true,
                                        100,
                                        true)));
        CachedRouteRoleRuleProvider provider = new CachedRouteRoleRuleProvider(source);
        DynamicRouteRoleAuthorizationManager manager =
                new DynamicRouteRoleAuthorizationManager(provider, properties(true));

        AuthorizationDecision decision =
                (AuthorizationDecision)
                        manager.authorize(Mono.empty(), context("GET", "/public/ping")).block();

        assertThat(decision).isNotNull();
        assertThat(decision.isGranted()).isTrue();
    }

    @Test
    void roleBasedRuleAllowsAndDeniesByAuthorities() {
        TestRuleSource source =
                new TestRuleSource(
                        List.of(
                                new RouteRoleRule(
                                        "admin",
                                        "POST",
                                        "/api/admin/**",
                                        List.of("ADMIN"),
                                        false,
                                        100,
                                        true)));
        CachedRouteRoleRuleProvider provider = new CachedRouteRoleRuleProvider(source);
        DynamicRouteRoleAuthorizationManager manager =
                new DynamicRouteRoleAuthorizationManager(provider, properties(true));

        AuthorizationDecision allowed =
                (AuthorizationDecision)
                        manager.authorize(
                                        Mono.just(authentication("ROLE_ADMIN")),
                                        context("POST", "/api/admin/users"))
                                .block();
        AuthorizationDecision denied =
                (AuthorizationDecision)
                        manager.authorize(
                                        Mono.just(authentication("ROLE_OPERATOR")),
                                        context("POST", "/api/admin/users"))
                                .block();

        assertThat(allowed).isNotNull();
        assertThat(allowed.isGranted()).isTrue();
        assertThat(denied).isNotNull();
        assertThat(denied.isGranted()).isFalse();
    }

    @Test
    void noMatchUsesDefaultDeny() {
        TestRuleSource source =
                new TestRuleSource(
                        List.of(
                                new RouteRoleRule(
                                        "known",
                                        "GET",
                                        "/api/known/**",
                                        List.of("ADMIN"),
                                        false,
                                        1,
                                        true)));
        CachedRouteRoleRuleProvider provider = new CachedRouteRoleRuleProvider(source);
        DynamicRouteRoleAuthorizationManager manager =
                new DynamicRouteRoleAuthorizationManager(provider, properties(true));

        AuthorizationDecision decision =
                (AuthorizationDecision)
                        manager.authorize(
                                        Mono.just(authentication("ROLE_ADMIN")),
                                        context("GET", "/api/unknown"))
                                .block();

        assertThat(decision).isNotNull();
        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    void refreshUpdatesBehaviorWithoutRestart() {
        TestRuleSource source =
                new TestRuleSource(
                        List.of(
                                new RouteRoleRule(
                                        "admin-only",
                                        "GET",
                                        "/api/report/**",
                                        List.of("ADMIN"),
                                        false,
                                        100,
                                        true)));
        CachedRouteRoleRuleProvider provider = new CachedRouteRoleRuleProvider(source);
        DynamicRouteRoleAuthorizationManager manager =
                new DynamicRouteRoleAuthorizationManager(provider, properties(true));

        AuthorizationDecision beforeRefresh =
                (AuthorizationDecision)
                        manager.authorize(
                                        Mono.just(authentication("ROLE_OPERATOR")),
                                        context("GET", "/api/report/daily"))
                                .block();

        source.setRules(
                List.of(
                        new RouteRoleRule(
                                "operator-now",
                                "GET",
                                "/api/report/**",
                                List.of("OPERATOR"),
                                false,
                                100,
                                true)));
        provider.refresh();

        AuthorizationDecision afterRefresh =
                (AuthorizationDecision)
                        manager.authorize(
                                        Mono.just(authentication("ROLE_OPERATOR")),
                                        context("GET", "/api/report/daily"))
                                .block();

        assertThat(beforeRefresh).isNotNull();
        assertThat(beforeRefresh.isGranted()).isFalse();
        assertThat(afterRefresh).isNotNull();
        assertThat(afterRefresh.isGranted()).isTrue();
    }

    @Test
    void highestPriorityRuleWinsOnConflict() {
        TestRuleSource source =
                new TestRuleSource(
                        List.of(
                                new RouteRoleRule(
                                        "broad-deny",
                                        "GET",
                                        "/api/**",
                                        List.of("ADMIN"),
                                        false,
                                        10,
                                        true),
                                new RouteRoleRule(
                                        "specific-allow",
                                        "GET",
                                        "/api/reports/**",
                                        List.of("OPERATOR"),
                                        false,
                                        100,
                                        true)));
        CachedRouteRoleRuleProvider provider = new CachedRouteRoleRuleProvider(source);
        DynamicRouteRoleAuthorizationManager manager =
                new DynamicRouteRoleAuthorizationManager(provider, properties(true));

        AuthorizationDecision decision =
                (AuthorizationDecision)
                        manager.authorize(
                                        Mono.just(authentication("ROLE_OPERATOR")),
                                        context("GET", "/api/reports/monthly"))
                                .block();

        assertThat(decision).isNotNull();
        assertThat(decision.isGranted()).isTrue();
    }

    private static AuthorizationContext context(String method, String path) {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.method(HttpMethod.valueOf(method), path));
        return new AuthorizationContext(exchange);
    }

    private static UsernamePasswordAuthenticationToken authentication(String... roles) {
        List<SimpleGrantedAuthority> authorities =
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken("user", "n/a", authorities);
    }

    private static RbacProperties properties(boolean defaultDeny) {
        RbacProperties properties = new RbacProperties();
        properties.setEnabled(true);
        properties.setDefaultDeny(defaultDeny);
        return properties;
    }

    private static final class TestRuleSource implements RouteRoleRuleSource {

        private final AtomicReference<List<RouteRoleRule>> rules;

        TestRuleSource(List<RouteRoleRule> initialRules) {
            this.rules = new AtomicReference<>(initialRules);
        }

        @Override
        public List<RouteRoleRule> loadRules() {
            return rules.get();
        }

        void setRules(List<RouteRoleRule> newRules) {
            this.rules.set(newRules);
        }
    }
}
