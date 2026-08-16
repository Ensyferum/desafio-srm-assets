package com.srm.gateway.security.rbac;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class CachedRouteRoleRuleProviderTest {

    @Test
    void keepsLastKnownGoodSnapshotWhenRefreshFails() {
        AtomicBoolean failRefresh = new AtomicBoolean(false);
        RouteRoleRuleSource source =
                () -> {
                    if (failRefresh.get()) {
                        throw new IllegalStateException("boom");
                    }
                    return List.of(
                            new RouteRoleRule(
                                    "rule-1", "GET", "/api/**", List.of("ADMIN"), false, 10, true));
                };

        CachedRouteRoleRuleProvider provider = new CachedRouteRoleRuleProvider(source);
        RouteRoleRulesSnapshot initial = provider.getSnapshot();

        failRefresh.set(true);
        RouteRoleRuleRefreshResult result = provider.refresh();

        assertThat(result.refreshed()).isFalse();
        assertThat(provider.getSnapshot().version()).isEqualTo(initial.version());
        assertThat(provider.getSnapshot().rules()).hasSize(1);
    }

    @Test
    void sortsRulesByPriorityDescending() {
        RouteRoleRuleSource source =
                () ->
                        List.of(
                                new RouteRoleRule(
                                        "low", "GET", "/api/**", List.of("ADMIN"), false, 1, true),
                                new RouteRoleRule(
                                        "high",
                                        "GET",
                                        "/api/reports/**",
                                        List.of("ADMIN"),
                                        false,
                                        100,
                                        true));

        CachedRouteRoleRuleProvider provider = new CachedRouteRoleRuleProvider(source);

        assertThat(provider.getSnapshot().rules())
                .extracting(RouteRoleRule::id)
                .containsExactly("high", "low");
    }
}
