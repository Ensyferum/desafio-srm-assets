package com.srm.gateway.security.rbac;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import reactor.core.publisher.Mono;

/** Manager de autorização reativo para regras dinâmicas route-vs-role. */
@Component
public class DynamicRouteRoleAuthorizationManager
        implements ReactiveAuthorizationManager<AuthorizationContext> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DynamicRouteRoleAuthorizationManager.class);

    private final RouteRoleRuleProvider provider;
    private final RbacProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public DynamicRouteRoleAuthorizationManager(
            RouteRoleRuleProvider provider, RbacProperties properties) {
        this.provider = provider;
        this.properties = properties;
    }

    @Override
    public Mono<AuthorizationResult> authorize(
            Mono<Authentication> authentication, AuthorizationContext context) {
        return authentication
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .map(
                        maybeAuth -> {
                            String requestMethod =
                                    context.getExchange().getRequest().getMethod() == null
                                            ? "UNKNOWN"
                                            : context.getExchange().getRequest().getMethod().name();
                            String requestPath =
                                    context.getExchange()
                                            .getRequest()
                                            .getPath()
                                            .pathWithinApplication()
                                            .value();
                            return evaluate(maybeAuth.orElse(null), requestMethod, requestPath);
                        });
    }

    private AuthorizationDecision evaluate(
            Authentication authentication, String requestMethod, String requestPath) {
        boolean isAuthenticated = isAuthenticated(authentication);

        if (!properties.isEnabled()) {
            logDecision(
                    isAuthenticated,
                    requestMethod,
                    requestPath,
                    "rbac-disabled",
                    provider.getSnapshot().version(),
                    "fallback-authenticated");
            return new AuthorizationDecision(isAuthenticated);
        }

        RouteRoleRulesSnapshot snapshot = provider.getSnapshot();
        RouteRoleRule matchedRule =
                snapshot.rules().stream()
                        .filter(rule -> rule.matches(requestMethod, requestPath, pathMatcher))
                        .findFirst()
                        .orElse(null);

        if (matchedRule == null) {
            boolean granted = !properties.isDefaultDeny() && isAuthenticated;
            logDecision(
                    granted,
                    requestMethod,
                    requestPath,
                    "no-rule",
                    snapshot.version(),
                    "default-policy");
            return new AuthorizationDecision(granted);
        }

        if (matchedRule.permitAll()) {
            logDecision(
                    true,
                    requestMethod,
                    requestPath,
                    matchedRule.id(),
                    snapshot.version(),
                    "permitAll");
            return new AuthorizationDecision(true);
        }

        if (!isAuthenticated) {
            logDecision(
                    false,
                    requestMethod,
                    requestPath,
                    matchedRule.id(),
                    snapshot.version(),
                    "unauthenticated");
            return new AuthorizationDecision(false);
        }

        if (matchedRule.roles().isEmpty()) {
            logDecision(
                    true,
                    requestMethod,
                    requestPath,
                    matchedRule.id(),
                    snapshot.version(),
                    "authenticated");
            return new AuthorizationDecision(true);
        }

        Set<String> authorities =
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(this::normalizeRole)
                        .collect(Collectors.toSet());

        boolean granted =
                matchedRule.roles().stream()
                        .map(this::normalizeRole)
                        .anyMatch(authorities::contains);

        logDecision(
                granted,
                requestMethod,
                requestPath,
                matchedRule.id(),
                snapshot.version(),
                granted ? "role-match" : "role-mismatch");
        return new AuthorizationDecision(granted);
    }

    private void logDecision(
            boolean granted,
            String method,
            String path,
            String ruleId,
            String version,
            String reason) {
        LOGGER.info(
                "rbac_decision outcome={} method={} path={} ruleId={} version={} reason={}",
                granted ? "ALLOW" : "DENY",
                method,
                path,
                ruleId,
                version,
                reason);
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }
}
