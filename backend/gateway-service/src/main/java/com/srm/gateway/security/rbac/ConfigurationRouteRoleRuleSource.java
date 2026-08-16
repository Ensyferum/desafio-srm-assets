package com.srm.gateway.security.rbac;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Fonte padrão de regras baseada em propriedades externas (YAML/JSON). */
@Component
public class ConfigurationRouteRoleRuleSource implements RouteRoleRuleSource {

    private final RbacProperties properties;

    public ConfigurationRouteRoleRuleSource(RbacProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<RouteRoleRule> loadRules() {
        List<RbacProperties.RuleProperties> configuredRules = properties.getRules();
        List<RouteRoleRule> rules = new ArrayList<>(configuredRules.size());
        for (int index = 0; index < configuredRules.size(); index++) {
            RbacProperties.RuleProperties configuredRule = configuredRules.get(index);
            String ruleId =
                    configuredRule.getId() == null || configuredRule.getId().isBlank()
                            ? "config-rule-" + index
                            : configuredRule.getId();
            rules.add(
                    new RouteRoleRule(
                            ruleId,
                            configuredRule.getMethod(),
                            configuredRule.getPathPattern(),
                            configuredRule.getRoles(),
                            configuredRule.isPermitAll(),
                            configuredRule.getPriority(),
                            configuredRule.isEnabled()));
        }
        return rules;
    }
}
