package com.srm.analytics.repo;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.analytics.repo.SettlementQueryBuilder.SqlQuery;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class SettlementQueryBuilderTest {

    private static final LocalDate START = LocalDate.of(2026, 8, 1);
    private static final LocalDate END = LocalDate.of(2026, 8, 31);

    @Test
    void baseQueryAlwaysFiltersCompleted() {
        SqlQuery query =
                SettlementQueryBuilder.select(null, null, null, null, PageRequest.of(0, 20));

        assertThat(query.sql()).contains("p.status = 'COMPLETED'");
        assertThat(query.sql()).contains("ORDER BY p.settled_at DESC");
        assertThat(query.sql()).contains("LIMIT ? OFFSET ?");
        assertThat(query.args()).containsExactly(20, 0L);
    }

    @Test
    void addsDateRangeClauses() {
        SqlQuery query =
                SettlementQueryBuilder.select(START, END, null, null, PageRequest.of(1, 10));

        assertThat(query.sql()).contains("p.settled_at >= ?");
        assertThat(query.sql()).contains("p.settled_at < ?");
        assertThat(query.args())
                .contains(START.atStartOfDay(), END.plusDays(1).atStartOfDay(), 10, 10L);
    }

    @Test
    void addsCedenteAndCurrencyFilters() {
        SqlQuery query =
                SettlementQueryBuilder.select(
                        START, null, "11222333000181", "USD", PageRequest.of(0, 20));

        assertThat(query.sql()).contains("p.cedente_document = ?");
        assertThat(query.sql()).contains("p.settlement_currency = ?");
        assertThat(query.args()).contains("11222333000181", "USD");
    }

    @Test
    void countQueryReusesSameFilters() {
        SqlQuery count = SettlementQueryBuilder.count(START, END, "11222333000181", "BRL");

        assertThat(count.sql()).startsWith("SELECT COUNT(*)");
        assertThat(count.args())
                .contains(
                        START.atStartOfDay(),
                        END.plusDays(1).atStartOfDay(),
                        "11222333000181",
                        "BRL");
    }
}
