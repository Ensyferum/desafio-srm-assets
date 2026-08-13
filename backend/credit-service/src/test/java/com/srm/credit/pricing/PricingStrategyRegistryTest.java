package com.srm.credit.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PricingStrategyRegistryTest {

    @Test
    void resolvesStrategyByReceivableTypeName() {
        PricingStrategyRegistry registry =
                new PricingStrategyRegistry(
                        List.of(new StandardPricingStrategy(), new ChequePreDatadoStrategy()));

        assertThat(registry.resolve("Cheque Pré-datado"))
                .isInstanceOf(ChequePreDatadoStrategy.class);
        assertThat(registry.resolve("Duplicata Mercantil"))
                .isInstanceOf(StandardPricingStrategy.class);
    }

    @Test
    void unknownTypeFallsBackToDefault() {
        PricingStrategyRegistry registry =
                new PricingStrategyRegistry(List.of(new StandardPricingStrategy()));

        assertThat(registry.resolve("Tipo Inexistente"))
                .isInstanceOf(StandardPricingStrategy.class);
    }

    @Test
    void registeredReturnsCopy() {
        PricingStrategyRegistry registry =
                new PricingStrategyRegistry(List.of(new ChequePreDatadoStrategy()));

        assertThat(registry.registered()).containsKey("Cheque Pré-datado");
    }
}
