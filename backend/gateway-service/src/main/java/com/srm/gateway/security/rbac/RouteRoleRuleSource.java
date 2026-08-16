package com.srm.gateway.security.rbac;

import java.util.List;

/** Fonte externa de regras Route-vs-Role. */
public interface RouteRoleRuleSource {

    List<RouteRoleRule> loadRules();
}
