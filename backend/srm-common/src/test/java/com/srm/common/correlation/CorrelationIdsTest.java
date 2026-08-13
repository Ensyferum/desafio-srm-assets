package com.srm.common.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CorrelationIdsTest {

    @AfterEach
    void cleanUp() {
        CorrelationIds.clear();
    }

    @Test
    void setAndGetStoresInMdc() {
        CorrelationIds.set("cid-123");
        assertThat(CorrelationIds.get()).isEqualTo("cid-123");
    }

    @Test
    void getOrCreateGeneratesUuidWhenAbsent() {
        String id = CorrelationIds.getOrCreate();
        assertThat(id).isNotBlank();
        assertThat(CorrelationIds.get()).isEqualTo(id);
    }

    @Test
    void getOrCreateReusesExistingValue() {
        CorrelationIds.set("existing");
        assertThat(CorrelationIds.getOrCreate()).isEqualTo("existing");
    }

    @Test
    void sanitizesOversizedValues() {
        CorrelationIds.set("x".repeat(200));
        assertThat(CorrelationIds.get()).hasSize(64);
    }

    @Test
    void ignoresBlankValues() {
        CorrelationIds.set("   ");
        assertThat(CorrelationIds.get()).isNull();
    }

    @Test
    void clearRemovesValueFromMdc() {
        CorrelationIds.set("cid");
        CorrelationIds.clear();
        assertThat(CorrelationIds.get()).isNull();
    }
}
