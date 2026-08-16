package com.srm.gateway.security.rbac;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Provider com cache atômico e política de último snapshot válido. */
@Component
public class CachedRouteRoleRuleProvider implements RouteRoleRuleProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedRouteRoleRuleProvider.class);

    private static final Comparator<RouteRoleRule> RULE_ORDER =
            Comparator.comparingInt(RouteRoleRule::priority)
                    .reversed()
                    .thenComparing(rule -> rule.pathPattern().length(), Comparator.reverseOrder())
                    .thenComparing(RouteRoleRule::id);

    private final RouteRoleRuleSource source;
    private final AtomicReference<RouteRoleRulesSnapshot> snapshotRef;

    public CachedRouteRoleRuleProvider(RouteRoleRuleSource source) {
        this.source = source;
        this.snapshotRef = new AtomicReference<>(new RouteRoleRulesSnapshot(List.of(), "empty"));
        refresh();
    }

    @Override
    public RouteRoleRulesSnapshot getSnapshot() {
        return snapshotRef.get();
    }

    @Override
    public RouteRoleRuleRefreshResult refresh() {
        try {
            List<RouteRoleRule> sortedRules =
                    source.loadRules().stream()
                            .filter(RouteRoleRule::enabled)
                            .sorted(RULE_ORDER)
                            .toList();
            String version = Instant.now().toString();
            RouteRoleRulesSnapshot newSnapshot = new RouteRoleRulesSnapshot(sortedRules, version);
            snapshotRef.set(newSnapshot);
            LOGGER.info(
                    "rbac_refresh status=success version={} rulesCount={}",
                    version,
                    sortedRules.size());
            return new RouteRoleRuleRefreshResult(
                    true, newSnapshot.version(), newSnapshot.rules().size(), "refresh-success");
        } catch (Exception exception) {
            RouteRoleRulesSnapshot currentSnapshot = snapshotRef.get();
            LOGGER.error(
                    "rbac_refresh status=failure version={} rulesCount={} message={}",
                    currentSnapshot.version(),
                    currentSnapshot.rules().size(),
                    exception.getMessage(),
                    exception);
            return new RouteRoleRuleRefreshResult(
                    false,
                    currentSnapshot.version(),
                    currentSnapshot.rules().size(),
                    "refresh-failure-keeping-last-good");
        }
    }
}
