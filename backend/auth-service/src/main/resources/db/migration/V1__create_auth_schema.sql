-- ============================================
-- SRM Credit Engine — Auth Service
-- Schema: auth
-- ============================================

CREATE TABLE users (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(100) NOT NULL,
    full_name  VARCHAR(120) NOT NULL,
    role       VARCHAR(20)  NOT NULL CHECK (role IN ('OPERATOR', 'MANAGER', 'ADMIN')),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
