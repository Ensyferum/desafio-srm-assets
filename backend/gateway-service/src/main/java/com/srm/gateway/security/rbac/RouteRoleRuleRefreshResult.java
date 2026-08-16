package com.srm.gateway.security.rbac;

/** Resultado da operação de refresh de regras RBAC. */
public record RouteRoleRuleRefreshResult(
        boolean refreshed, String version, int rulesCount, String message) {}
