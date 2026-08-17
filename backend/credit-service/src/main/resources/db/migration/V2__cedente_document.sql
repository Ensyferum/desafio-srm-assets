-- ============================================
-- SRM Credit Engine — Credit Service
-- Migration V2: cedente identificado pelo documento (CNPJ), não pelo ID
-- ============================================

ALTER TABLE receivable ADD COLUMN cedente_document VARCHAR(14);

-- backfill dos registros existentes com documento neutro
UPDATE receivable SET cedente_document = '00000000000000' WHERE cedente_document IS NULL;

ALTER TABLE receivable ALTER COLUMN cedente_document SET NOT NULL;

DROP INDEX idx_receivable_cedente;
ALTER TABLE receivable DROP COLUMN cedente_id;

CREATE INDEX idx_receivable_cedente_document ON receivable(cedente_document);
