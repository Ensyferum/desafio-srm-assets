-- ============================================
-- SRM Credit Engine — Analytics Service
-- Schema: analytics (projeções de leitura — CQRS)
-- ============================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Projeção denormalizada do extrato de liquidações (RF05)
CREATE TABLE settlement_projection (
    transaction_id        UUID PRIMARY KEY,
    receivable_id         UUID NOT NULL,
    cedente_id            UUID NOT NULL,
    face_value            DECIMAL(20, 2) NOT NULL,
    present_value         DECIMAL(20, 2) NOT NULL,
    discount_value        DECIMAL(20, 2) NOT NULL,
    currency              VARCHAR(3) NOT NULL,
    settlement_currency   VARCHAR(3) NOT NULL,
    exchange_rate_applied DECIMAL(20, 10),
    status                VARCHAR(20) NOT NULL,
    settled_at            TIMESTAMP WITH TIME ZONE,
    created_by            VARCHAR(100),
    correlation_id        VARCHAR(64),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_projection_settled_at ON settlement_projection(settled_at DESC);
CREATE INDEX idx_projection_cedente ON settlement_projection(cedente_id);
CREATE INDEX idx_projection_settlement_currency ON settlement_projection(settlement_currency);

-- Resumo diário agregado por moeda (dashboard)
CREATE TABLE settlement_daily_summary (
    summary_date         DATE NOT NULL,
    currency             VARCHAR(3) NOT NULL,
    total_transactions   BIGINT NOT NULL DEFAULT 0,
    total_present_value  DECIMAL(20, 2) NOT NULL DEFAULT 0,
    total_discount_value DECIMAL(20, 2) NOT NULL DEFAULT 0,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (summary_date, currency)
);
