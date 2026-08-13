package com.srm.common.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;

/** Adiciona tags comuns (application) a todas as métricas expostas via Prometheus. */
@AutoConfiguration
public class MetricsAutoConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> srmCommonTags(
            @Value("${spring.application.name:unknown}") String applicationName) {
        return registry -> registry.config().commonTags("application", applicationName);
    }
}
