package com.srm.credit.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/** Métricas customizadas de liquidação (RNF02). */
@Component
public class SettlementMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Counter> settlementCounters = new ConcurrentHashMap<>();
    private final Counter simulationCounter;

    public SettlementMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.simulationCounter =
                Counter.builder("srm.pricing.simulations")
                        .description("Total de simulações de precificação")
                        .register(meterRegistry);
    }

    public void countSettlement(String settlementCurrency) {
        settlementCounters
                .computeIfAbsent(
                        settlementCurrency,
                        currency ->
                                Counter.builder("srm.settlements.total")
                                        .description("Liquidações concluídas por moeda")
                                        .tag("currency", currency)
                                        .register(meterRegistry))
                .increment();
    }

    public void countSimulation() {
        simulationCounter.increment();
    }
}
