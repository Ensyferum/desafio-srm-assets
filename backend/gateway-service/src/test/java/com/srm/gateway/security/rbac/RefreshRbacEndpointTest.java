package com.srm.gateway.security.rbac;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RefreshRbacEndpointTest {

    @Test
    void refreshEndpointReturnsStatusMetadata() {
        StubProvider provider = new StubProvider();
        RefreshRbacEndpoint endpoint = new RefreshRbacEndpoint(provider);

        Map<String, Object> before = endpoint.status();
        Map<String, Object> after = endpoint.refresh();

        assertThat(before.get("version")).isEqualTo("v1");
        assertThat(before.get("rulesCount")).isEqualTo(1);
        assertThat(after.get("refreshed")).isEqualTo(true);
        assertThat(after.get("version")).isEqualTo("v2");
        assertThat(after.get("rulesCount")).isEqualTo(2);
    }

    private static final class StubProvider implements RouteRoleRuleProvider {

        private RouteRoleRulesSnapshot snapshot =
                new RouteRoleRulesSnapshot(
                        List.of(
                                new RouteRoleRule(
                                        "first", "GET", "/api/**", List.of(), false, 1, true)),
                        "v1");

        @Override
        public RouteRoleRulesSnapshot getSnapshot() {
            return snapshot;
        }

        @Override
        public RouteRoleRuleRefreshResult refresh() {
            snapshot =
                    new RouteRoleRulesSnapshot(
                            List.of(
                                    new RouteRoleRule(
                                            "first", "GET", "/api/**", List.of(), false, 1, true),
                                    new RouteRoleRule(
                                            "second",
                                            "POST",
                                            "/api/admin/**",
                                            List.of("ADMIN"),
                                            false,
                                            100,
                                            true)),
                            "v2");
            return new RouteRoleRuleRefreshResult(true, "v2", 2, "refresh-success");
        }
    }
}
