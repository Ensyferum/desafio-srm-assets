package com.srm.credit.pricing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Registro de estratégias: resolve a estratégia pelo nome do tipo de recebível. */
@Component
public class PricingStrategyRegistry {

    private final Map<String, PricingStrategy> strategies = new HashMap<>();
    private final PricingStrategy defaultStrategy;

    public PricingStrategyRegistry(List<PricingStrategy> all) {
        PricingStrategy fallback = new StandardPricingStrategy();
        for (PricingStrategy strategy : all) {
            if (strategy.supports() == null) {
                fallback = strategy;
            } else {
                strategies.put(strategy.supports(), strategy);
            }
        }
        this.defaultStrategy = fallback;
    }

    public PricingStrategy resolve(String receivableTypeName) {
        return strategies.getOrDefault(receivableTypeName, defaultStrategy);
    }

    public Map<String, PricingStrategy> registered() {
        return Map.copyOf(strategies);
    }
}
