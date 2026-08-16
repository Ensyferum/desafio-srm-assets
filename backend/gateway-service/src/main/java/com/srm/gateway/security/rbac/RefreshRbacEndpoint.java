package com.srm.gateway.security.rbac;

import java.util.Map;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

/** Endpoint actuator interno para refresh manual de regras RBAC. */
@Component
@Endpoint(id = "refresh-rbac")
public class RefreshRbacEndpoint {

    private final RouteRoleRuleProvider provider;

    public RefreshRbacEndpoint(RouteRoleRuleProvider provider) {
        this.provider = provider;
    }

    @ReadOperation
    public Map<String, Object> status() {
        RouteRoleRulesSnapshot snapshot = provider.getSnapshot();
        return Map.of(
                "status", "ok",
                "version", snapshot.version(),
                "rulesCount", snapshot.rules().size());
    }

    @WriteOperation
    public Map<String, Object> refresh() {
        RouteRoleRuleRefreshResult result = provider.refresh();
        return Map.of(
                "refreshed", result.refreshed(),
                "version", result.version(),
                "rulesCount", result.rulesCount(),
                "message", result.message());
    }
}
