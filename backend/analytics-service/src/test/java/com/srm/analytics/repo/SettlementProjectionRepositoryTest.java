package com.srm.analytics.repo;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.srm.common.event.SettlementEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SettlementProjectionRepositoryTest {

    @Mock private JdbcTemplate jdbcTemplate;

    private SettlementEvent event() {
        return new SettlementEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000.00"),
                new BigDecimal("94232.23"),
                new BigDecimal("5767.77"),
                "BRL",
                "USD",
                new BigDecimal("5.4523"),
                Instant.parse("2026-08-12T22:30:00Z"),
                "COMPLETED");
    }

    @Test
    void upsertPersistsProjectionRow() {
        SettlementProjectionRepository repository =
                new SettlementProjectionRepository(jdbcTemplate);
        SettlementEvent event = event();

        repository.upsert(event);

        verify(jdbcTemplate)
                .update(
                        anyString(),
                        eq(event.transactionId()),
                        eq(event.receivableId()),
                        eq(event.cedenteId()),
                        eq(event.faceValue()),
                        eq(event.presentValue()),
                        eq(event.discountValue()),
                        eq(event.currency()),
                        eq(event.settlementCurrency()),
                        eq(event.exchangeRateApplied()),
                        eq(event.status()),
                        eq(event.settledAt()));
    }

    @Test
    void updateDailySummaryUsesSettledDate() {
        SettlementProjectionRepository repository =
                new SettlementProjectionRepository(jdbcTemplate);
        SettlementEvent event = event();

        repository.updateDailySummary(event);

        verify(jdbcTemplate)
                .update(
                        anyString(),
                        eq(LocalDate.of(2026, 8, 12)),
                        eq(event.settlementCurrency()),
                        eq(event.presentValue()),
                        eq(event.discountValue()));
    }

    @Test
    void updateDailySummaryFallsBackToTodayWhenSettledAtNull() {
        SettlementProjectionRepository repository =
                new SettlementProjectionRepository(jdbcTemplate);
        SettlementEvent base = event();
        SettlementEvent event =
                new SettlementEvent(
                        base.transactionId(),
                        base.receivableId(),
                        base.cedenteId(),
                        base.faceValue(),
                        base.presentValue(),
                        base.discountValue(),
                        base.currency(),
                        base.settlementCurrency(),
                        base.exchangeRateApplied(),
                        null,
                        base.status());

        repository.updateDailySummary(event);

        verify(jdbcTemplate)
                .update(
                        anyString(),
                        eq(LocalDate.now()),
                        eq(event.settlementCurrency()),
                        eq(event.presentValue()),
                        eq(event.discountValue()));
    }
}
