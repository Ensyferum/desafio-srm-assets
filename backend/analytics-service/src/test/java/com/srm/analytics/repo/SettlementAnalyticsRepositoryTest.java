package com.srm.analytics.repo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.analytics.dto.AnalyticsSummaryResponse;
import com.srm.analytics.dto.CedenteDistribution;
import com.srm.analytics.dto.PageResponse;
import com.srm.analytics.dto.TimeSeriesPoint;
import com.srm.analytics.dto.TransactionSummary;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

class SettlementAnalyticsRepositoryTest {

    @Test
    void rowMapperMapsAllColumns() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID recId = UUID.randomUUID();
        OffsetDateTime settledAt = OffsetDateTime.of(2026, 8, 12, 22, 30, 0, 0, ZoneOffset.UTC);

        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("transaction_id", UUID.class)).thenReturn(txId);
        when(rs.getObject("receivable_id", UUID.class)).thenReturn(recId);
        when(rs.getString("cedente_document")).thenReturn("11222333000181");
        when(rs.getBigDecimal("face_value")).thenReturn(new BigDecimal("100000.00"));
        when(rs.getBigDecimal("present_value")).thenReturn(new BigDecimal("94232.23"));
        when(rs.getBigDecimal("discount_value")).thenReturn(new BigDecimal("5767.77"));
        when(rs.getString("currency")).thenReturn("BRL");
        when(rs.getString("settlement_currency")).thenReturn("USD");
        when(rs.getBigDecimal("exchange_rate_applied")).thenReturn(new BigDecimal("5.4523"));
        when(rs.getString("status")).thenReturn("COMPLETED");
        when(rs.getObject("settled_at", OffsetDateTime.class)).thenReturn(settledAt);

        TransactionSummary summary =
                SettlementAnalyticsRepository.TRANSACTION_ROW_MAPPER.mapRow(rs, 0);

        assertThat(summary.transactionId()).isEqualTo(txId);
        assertThat(summary.cedenteDocument()).isEqualTo("11222333000181");
        assertThat(summary.presentValue()).isEqualByComparingTo("94232.23");
        assertThat(summary.settlementCurrency()).isEqualTo("USD");
        assertThat(summary.settledAt()).isEqualTo(settledAt.toInstant());
    }

    @Test
    void findSettlementsRunsCountAndSelectQueries() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SettlementAnalyticsRepository repository = new SettlementAnalyticsRepository(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(5L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        PageResponse<TransactionSummary> page =
                repository.findSettlements(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        null,
                        "BRL",
                        PageRequest.of(0, 20));

        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.content()).isEmpty();
    }

    @Test
    void summaryAggregatesDailyRowsByCurrency() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SettlementAnalyticsRepository repository = new SettlementAnalyticsRepository(jdbcTemplate);
        doAnswer(
                        inv -> {
                            RowCallbackHandler handler = inv.getArgument(1);
                            ResultSet rs = mock(ResultSet.class);
                            when(rs.getString("currency")).thenReturn("BRL");
                            when(rs.getBigDecimal("pv")).thenReturn(new BigDecimal("100.00"));
                            handler.processRow(rs);
                            return null;
                        })
                .when(jdbcTemplate)
                .query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(2L);
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(new BigDecimal("10.00"));

        AnalyticsSummaryResponse summary =
                repository.summary(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertThat(summary.totalTransactions()).isEqualTo(2);
        assertThat(summary.totalPresentValue()).isEqualByComparingTo("100.00");
        assertThat(summary.totalDiscountValue()).isEqualByComparingTo("10.00");
        assertThat(summary.presentValueByCurrency()).containsEntry("BRL", new BigDecimal("100.00"));
    }

    @Test
    void timeSeriesRowMapperMapsAllColumns() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("summary_date", LocalDate.class)).thenReturn(LocalDate.of(2026, 8, 12));
        when(rs.getString("currency")).thenReturn("BRL");
        when(rs.getLong("total_transactions")).thenReturn(2L);
        when(rs.getBigDecimal("total_present_value")).thenReturn(new BigDecimal("100.00"));

        TimeSeriesPoint point = SettlementAnalyticsRepository.TIME_SERIES_ROW_MAPPER.mapRow(rs, 0);

        assertThat(point.date()).isEqualTo(LocalDate.of(2026, 8, 12));
        assertThat(point.currency()).isEqualTo("BRL");
        assertThat(point.transactions()).isEqualTo(2);
        assertThat(point.presentValue()).isEqualByComparingTo("100.00");
    }

    @Test
    void cedenteRowMapperMapsAllColumns() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("cedente_document")).thenReturn("11222333000181");
        when(rs.getLong("total_transactions")).thenReturn(3L);
        when(rs.getBigDecimal("present_value")).thenReturn(new BigDecimal("150000.00"));

        CedenteDistribution distribution =
                SettlementAnalyticsRepository.CEDENTE_ROW_MAPPER.mapRow(rs, 0);

        assertThat(distribution.cedenteDocument()).isEqualTo("11222333000181");
        assertThat(distribution.transactions()).isEqualTo(3);
        assertThat(distribution.presentValue()).isEqualByComparingTo("150000.00");
    }

    @Test
    void timeSeriesQueriesDailySummaryRows() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SettlementAnalyticsRepository repository = new SettlementAnalyticsRepository(jdbcTemplate);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(
                        List.of(
                                new TimeSeriesPoint(
                                        LocalDate.of(2026, 8, 12),
                                        "BRL",
                                        2,
                                        new BigDecimal("100.00"))));

        List<TimeSeriesPoint> series =
                repository.timeSeries(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertThat(series).hasSize(1);
        assertThat(series.get(0).presentValue()).isEqualByComparingTo("100.00");
    }

    @Test
    void distributionByCedenteAggregatesProjectionRows() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SettlementAnalyticsRepository repository = new SettlementAnalyticsRepository(jdbcTemplate);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(
                        List.of(
                                new CedenteDistribution(
                                        "11222333000181", 3, new BigDecimal("150000.00"))));

        List<CedenteDistribution> distribution = repository.distributionByCedente(start, end);

        assertThat(distribution).hasSize(1);
        assertThat(distribution.get(0).cedenteDocument()).isEqualTo("11222333000181");
        // O limite superior deve ser exclusivo (fim + 1 dia) para incluir o dia inteiro
        verify(jdbcTemplate)
                .query(anyString(), any(RowMapper.class), eq(start), eq(end.plusDays(1)));
    }

    @Test
    void pageResponseComputesMetadata() {
        List<String> content = List.of("a", "b");

        PageResponse<String> page = PageResponse.of(content, PageRequest.of(0, 20), 42);

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(42);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isFalse();
    }

    @Test
    void pageResponseMarksLastPage() {
        PageResponse<String> page = PageResponse.of(List.of("x"), PageRequest.of(2, 20), 42);

        assertThat(page.page()).isEqualTo(2);
        assertThat(page.last()).isTrue();
        assertThat(page.first()).isFalse();
    }
}
