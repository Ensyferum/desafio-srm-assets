-- ============================================
-- SRM Credit Engine — Currency Service
-- Schema: currency
-- ============================================

CREATE TABLE currency (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code       VARCHAR(3)  NOT NULL UNIQUE CHECK (code IN ('BRL', 'USD')),
    name       VARCHAR(50) NOT NULL,
    symbol     VARCHAR(5)  NOT NULL,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exchange_rate (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency  VARCHAR(3)   NOT NULL REFERENCES currency(code),
    to_currency    VARCHAR(3)   NOT NULL REFERENCES currency(code),
    rate           DECIMAL(20, 10) NOT NULL CHECK (rate > 0),
    effective_date DATE         NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(100) NOT NULL,
    CONSTRAINT chk_currency_pair CHECK (from_currency <> to_currency),
    CONSTRAINT uk_currency_pair_date UNIQUE (from_currency, to_currency, effective_date)
);

CREATE INDEX idx_exchange_rate_pair_date
    ON exchange_rate(from_currency, to_currency, effective_date DESC);

-- Dados iniciais (seed)
INSERT INTO currency (code, name, symbol) VALUES
    ('BRL', 'Real Brasileiro', 'R$'),
    ('USD', 'Dólar Americano', '$');

INSERT INTO exchange_rate (from_currency, to_currency, rate, effective_date, created_by)
VALUES ('USD', 'BRL', 5.4500000000, CURRENT_DATE, 'system_seed');
