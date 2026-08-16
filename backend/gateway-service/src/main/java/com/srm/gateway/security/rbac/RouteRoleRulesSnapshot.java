package com.srm.gateway.security.rbac;

import java.util.List;

/** Snapshot imutável de regras e metadata de versão. */
public record RouteRoleRulesSnapshot(List<RouteRoleRule> rules, String version) {

    public RouteRoleRulesSnapshot {
        rules = List.copyOf(rules);
        version = version == null || version.isBlank() ? "unknown" : version;
    }
}
