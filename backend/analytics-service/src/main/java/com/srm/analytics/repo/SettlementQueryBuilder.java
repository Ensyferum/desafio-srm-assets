package com.srm.analytics.repo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/**
 * Constrói as queries otimizadas do extrato de liquidações (RF05) com filtros dinâmicos por
 * período, cedente e moeda, evitando SQL injection via bind params.
 */
final class SettlementQueryBuilder {

    private SettlementQueryBuilder() {}

    /** Query com SQL e argumentos de bind prontos para execução. */
    record SqlQuery(String sql, List<Object> args) {}

    static SqlQuery count(LocalDate startDate, LocalDate endDate, UUID cedenteId, String currency) {
        Conditions conditions = new Conditions(startDate, endDate, cedenteId, currency);
        return new SqlQuery(
                "SELECT COUNT(*) FROM analytics.settlement_projection p" + conditions.where(),
                conditions.args);
    }

    static SqlQuery select(
            LocalDate startDate,
            LocalDate endDate,
            UUID cedenteId,
            String currency,
            Pageable pageable) {
        Conditions conditions = new Conditions(startDate, endDate, cedenteId, currency);
        StringBuilder sql =
                new StringBuilder(
                        """
                SELECT p.transaction_id, p.receivable_id, p.cedente_id, p.face_value,
                       p.present_value, p.discount_value, p.currency, p.settlement_currency,
                       p.exchange_rate_applied, p.status, p.settled_at
                FROM analytics.settlement_projection p
                """);
        sql.append(conditions.where());
        sql.append(" ORDER BY p.settled_at DESC, p.transaction_id DESC");
        sql.append(" LIMIT ? OFFSET ?");
        List<Object> args = new ArrayList<>(conditions.args);
        args.add(pageable.getPageSize());
        args.add(pageable.getOffset());
        return new SqlQuery(sql.toString(), args);
    }

    private static final class Conditions {
        private final List<Object> args = new ArrayList<>();
        private final List<String> clauses = new ArrayList<>();

        Conditions(LocalDate startDate, LocalDate endDate, UUID cedenteId, String currency) {
            clauses.add("p.status = 'COMPLETED'");
            if (startDate != null) {
                clauses.add("p.settled_at >= ?");
                args.add(startDate.atStartOfDay());
            }
            if (endDate != null) {
                clauses.add("p.settled_at < ?");
                args.add(endDate.plusDays(1).atStartOfDay());
            }
            if (cedenteId != null) {
                clauses.add("p.cedente_id = ?");
                args.add(cedenteId);
            }
            if (currency != null && !currency.isBlank()) {
                clauses.add("p.settlement_currency = ?");
                args.add(currency);
            }
        }

        String where() {
            return " WHERE " + String.join(" AND ", clauses);
        }
    }
}
