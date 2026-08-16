package com.srm.gateway.security.rbac;

import java.util.List;
import java.util.Locale;
import org.springframework.util.AntPathMatcher;

/** Regra de autorização Route-vs-Role para o gateway. */
public record RouteRoleRule(
        String id,
        String method,
        String pathPattern,
        List<String> roles,
        boolean permitAll,
        int priority,
        boolean enabled) {

    public RouteRoleRule {
        id = id == null || id.isBlank() ? "unnamed-rule" : id;
        method = method == null || method.isBlank() ? "*" : method.toUpperCase(Locale.ROOT);
        pathPattern = pathPattern == null || pathPattern.isBlank() ? "/**" : pathPattern.trim();
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public boolean matches(String requestMethod, String requestPath, AntPathMatcher matcher) {
        boolean methodMatches = "*".equals(method) || method.equalsIgnoreCase(requestMethod);
        return methodMatches && matcher.match(pathPattern, requestPath);
    }
}
