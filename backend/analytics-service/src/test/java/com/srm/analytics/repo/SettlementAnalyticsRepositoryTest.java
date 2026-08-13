package com.srm.analytics.repo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.srm.analytics.dto.PageResponse;
import com.srm.analytics.dto.TransactionSummary;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class SettlementAnalyticsRepositoryTest {

    @Test
    void rowMapperMapsAllColumns() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID recId = UUID.randomUUID();
        UUID cedente = UUID.randomUUID();
        OffsetDateTime settledAt = OffsetDateTime.of(2026, 8, 12, 22, 30, 0, 0, ZoneOffset.UTC);

        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("transaction_id", UUID.class)).thenReturn(txId);
        when(rs.getObject("receivable_id", UUID.class)).thenReturn(recId);
        when(rs.getObject("cedente_id", UUID.class)).thenReturn(cedente);
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
        assertThat(summary.cedenteId()).isEqualTo(cedente);
        assertThat(summary.presentValue()).isEqualByComparingTo("94232.23");
        assertThat(summary.settlementCurrency()).isEqualTo("USD");
        assertThat(summary.settledAt()).isEqualTo(settledAt.toInstant());
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
