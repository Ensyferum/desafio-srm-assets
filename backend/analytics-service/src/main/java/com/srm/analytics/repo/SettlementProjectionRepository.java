package com.srm.analytics.repo;

import com.srm.common.event.SettlementEvent;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Projeção de leitura (CQRS) alimentada pelos eventos {@code settlement.events}. */
@Repository
public class SettlementProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public SettlementProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void upsert(SettlementEvent event) {
        jdbcTemplate.update(
                """
                INSERT INTO analytics.settlement_projection (
                    transaction_id, receivable_id, cedente_id, face_value, present_value,
                    discount_value, currency, settlement_currency, exchange_rate_applied,
                    status, settled_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (transaction_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    updated_at = CURRENT_TIMESTAMP
                """,
                event.transactionId(),
                event.receivableId(),
                event.cedenteId(),
                event.faceValue(),
                event.presentValue(),
                event.discountValue(),
                event.currency(),
                event.settlementCurrency(),
                event.exchangeRateApplied(),
                event.status(),
                event.settledAt());
    }

    @Transactional
    public void updateDailySummary(SettlementEvent event) {
        LocalDate settledDate =
                event.settledAt() == null
                        ? LocalDate.now()
                        : event.settledAt().atZone(ZoneOffset.UTC).toLocalDate();
        jdbcTemplate.update(
                """
                INSERT INTO analytics.settlement_daily_summary (
                    summary_date, currency, total_transactions, total_present_value,
                    total_discount_value, updated_at)
                VALUES (?, ?, 1, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (summary_date, currency) DO UPDATE SET
                    total_transactions = analytics.settlement_daily_summary.total_transactions + 1,
                    total_present_value = analytics.settlement_daily_summary.total_present_value + EXCLUDED.total_present_value,
                    total_discount_value = analytics.settlement_daily_summary.total_discount_value + EXCLUDED.total_discount_value,
                    updated_at = CURRENT_TIMESTAMP
                """,
                settledDate,
                event.settlementCurrency(),
                event.presentValue(),
                event.discountValue());
    }
}
