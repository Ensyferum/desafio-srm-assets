package com.srm.analytics.repo;

import com.srm.analytics.dto.AnalyticsSummaryResponse;
import com.srm.analytics.dto.PageResponse;
import com.srm.analytics.dto.TransactionSummary;
import com.srm.analytics.repo.SettlementQueryBuilder.SqlQuery;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Consultas analíticas via JDBC Template (RF05) — camada de 2 níveis, sem passar pelo ORM, para
 * performance em grandes volumes.
 */
@Repository
public class SettlementAnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;

    public SettlementAnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResponse<TransactionSummary> findSettlements(
            LocalDate startDate,
            LocalDate endDate,
            UUID cedenteId,
            String currency,
            Pageable pageable) {
        SqlQuery count = SettlementQueryBuilder.count(startDate, endDate, cedenteId, currency);
        Long total = jdbcTemplate.queryForObject(count.sql(), Long.class, count.args().toArray());

        SqlQuery select =
                SettlementQueryBuilder.select(startDate, endDate, cedenteId, currency, pageable);
        List<TransactionSummary> content =
                jdbcTemplate.query(select.sql(), TRANSACTION_ROW_MAPPER, select.args().toArray());

        return PageResponse.of(content, pageable, total == null ? 0 : total);
    }

    public AnalyticsSummaryResponse summary(LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> byCurrency = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                SELECT currency, COALESCE(SUM(total_present_value), 0) AS pv
                FROM analytics.settlement_daily_summary
                WHERE summary_date >= ? AND summary_date <= ?
                GROUP BY currency
                """,
                rs -> {
                    byCurrency.put(rs.getString("currency"), rs.getBigDecimal("pv"));
                },
                startDate,
                endDate);

        BigDecimal totalPv = byCurrency.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Long totalTransactions =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COALESCE(SUM(total_transactions), 0)
                        FROM analytics.settlement_daily_summary
                        WHERE summary_date >= ? AND summary_date <= ?
                        """,
                        Long.class,
                        startDate,
                        endDate);
        BigDecimal totalDiscount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COALESCE(SUM(total_discount_value), 0)
                        FROM analytics.settlement_daily_summary
                        WHERE summary_date >= ? AND summary_date <= ?
                        """,
                        BigDecimal.class,
                        startDate,
                        endDate);

        return new AnalyticsSummaryResponse(
                totalTransactions == null ? 0 : totalTransactions,
                totalPv,
                totalDiscount == null ? BigDecimal.ZERO : totalDiscount,
                byCurrency,
                startDate,
                endDate);
    }

    static final RowMapper<TransactionSummary> TRANSACTION_ROW_MAPPER =
            (ResultSet rs, int rowNum) ->
                    new TransactionSummary(
                            rs.getObject("transaction_id", UUID.class),
                            rs.getObject("receivable_id", UUID.class),
                            rs.getObject("cedente_id", UUID.class),
                            rs.getBigDecimal("face_value"),
                            rs.getBigDecimal("present_value"),
                            rs.getBigDecimal("discount_value"),
                            rs.getString("currency"),
                            rs.getString("settlement_currency"),
                            rs.getBigDecimal("exchange_rate_applied"),
                            rs.getString("status"),
                            toInstant(rs));

    private static java.time.Instant toInstant(ResultSet rs) throws SQLException {
        OffsetDateTime settledAt = rs.getObject("settled_at", OffsetDateTime.class);
        return settledAt == null ? null : settledAt.toInstant();
    }
}
