package com.srm.common.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MetricsAutoConfigurationTest {

    @Test
    void addsApplicationCommonTag() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new MetricsAutoConfiguration().srmCommonTags("currency-service").customize(registry);

        Counter counter = Counter.builder("srm.test.metric").register(registry);
        counter.increment();

        assertThat(
                        registry.get("srm.test.metric")
                                .tag("application", "currency-service")
                                .counter()
                                .count())
                .isEqualTo(1.0);
    }
}
