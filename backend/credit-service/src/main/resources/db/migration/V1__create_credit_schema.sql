-- ============================================
-- SRM Credit Engine — Credit Service
-- Schema: credit
-- ============================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE receivable_type (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name           VARCHAR(100) NOT NULL UNIQUE,
    spread_monthly DECIMAL(10, 6) NOT NULL CHECK (spread_monthly >= 0),
    description    TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE receivable (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cedente_id  UUID NOT NULL,
    type_id     UUID NOT NULL REFERENCES receivable_type(id),
    face_value  DECIMAL(20, 2) NOT NULL CHECK (face_value > 0),
    due_date    DATE NOT NULL CHECK (due_date > CURRENT_DATE),
    currency_id VARCHAR(3) NOT NULL CHECK (currency_id IN ('BRL', 'USD')),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                CHECK (status IN ('PENDING', 'PRICED', 'SETTLED', 'CANCELLED')),
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_receivable_cedente ON receivable(cedente_id);
CREATE INDEX idx_receivable_currency ON receivable(currency_id);
CREATE INDEX idx_receivable_status ON receivable(status);
CREATE INDEX idx_receivable_due_date ON receivable(due_date);

CREATE TABLE transaction (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    receivable_id         UUID NOT NULL REFERENCES receivable(id),
    present_value         DECIMAL(20, 2) NOT NULL CHECK (present_value > 0),
    discount_value        DECIMAL(20, 2) NOT NULL CHECK (discount_value >= 0),
    settlement_currency   VARCHAR(3) NOT NULL CHECK (settlement_currency IN ('BRL', 'USD')),
    exchange_rate_applied DECIMAL(20, 10),
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED')),
    version               BIGINT NOT NULL DEFAULT 0,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(100) NOT NULL,
    settled_at            TIMESTAMP WITH TIME ZONE,
    error_message         TEXT
);

CREATE INDEX idx_transaction_settlement_currency ON transaction(settlement_currency, settled_at DESC);
CREATE INDEX idx_transaction_receivable ON transaction(receivable_id);
CREATE INDEX idx_transaction_status ON transaction(status);

-- Tipos de recebíveis com spreads mensais (seed — RF02)
INSERT INTO receivable_type (name, spread_monthly, description) VALUES
    ('Duplicata Mercantil', 0.015000, 'Título de crédito emitido em operações comerciais'),
    ('Cheque Pré-datado', 0.025000, 'Cheque com data de vencimento futura'),
    ('Contrato de Prestação de Serviços', 0.020000, 'Recebível oriundo de contratos de serviços'),
    ('Nota Promissória', 0.018000, 'Título de crédito com promessa de pagamento');
