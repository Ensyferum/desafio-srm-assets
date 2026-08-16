package com.srm.gateway.security.rbac;

/** Provider de regras com snapshot em memória para leituras rápidas. */
public interface RouteRoleRuleProvider {

    RouteRoleRulesSnapshot getSnapshot();

    RouteRoleRuleRefreshResult refresh();
}
