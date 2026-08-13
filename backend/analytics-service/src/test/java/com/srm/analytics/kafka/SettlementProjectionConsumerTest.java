package com.srm.analytics.kafka;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.srm.analytics.repo.SettlementProjectionRepository;
import com.srm.common.event.SettlementEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SettlementProjectionConsumerTest {

    @Test
    void materializesProjectionAndDailySummary() {
        SettlementProjectionRepository repository = mock(SettlementProjectionRepository.class);
        SettlementProjectionConsumer consumer = new SettlementProjectionConsumer(repository);
        SettlementEvent event =
                new SettlementEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("100000.00"),
                        new BigDecimal("94232.23"),
                        new BigDecimal("5767.77"),
                        "BRL",
                        "USD",
                        new BigDecimal("5.4523"),
                        Instant.now(),
                        "COMPLETED");

        consumer.onSettlement(event);

        verify(repository).upsert(event);
        verify(repository).updateDailySummary(event);
    }
}
