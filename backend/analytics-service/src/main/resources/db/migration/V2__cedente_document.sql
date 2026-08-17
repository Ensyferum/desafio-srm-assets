-- ============================================
-- SRM Credit Engine — Analytics Service
-- Migration V2: projeção com cedente pelo documento (CNPJ)
-- ============================================

ALTER TABLE settlement_projection ADD COLUMN cedente_document VARCHAR(14);

-- backfill cruzando com o receivable do schema credit (mesma base, user srm_user)
UPDATE settlement_projection p
SET cedente_document = r.cedente_document
FROM credit.receivable r
WHERE p.receivable_id = r.id;

UPDATE settlement_projection SET cedente_document = '00000000000000'
WHERE cedente_document IS NULL;

ALTER TABLE settlement_projection ALTER COLUMN cedente_document SET NOT NULL;

DROP INDEX idx_projection_cedente;
ALTER TABLE settlement_projection DROP COLUMN cedente_id;

CREATE INDEX idx_projection_cedente_document ON settlement_projection(cedente_document);
