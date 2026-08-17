#!/usr/bin/env bash
# ============================================================
# SRM Credit Engine — Smoke test E2E
#
# Percorre o fluxo de negócio completo através do gateway:
#   login → tipos de recebíveis → taxa de câmbio (manager)
#   → simulação de preço → lote de recebíveis → liquidação
#   → extrato (analytics) + resumo (dashboard)
#
# Uso:
#   docker compose up -d --build
#   ./scripts/e2e-smoke.sh
#
# Requisitos: curl, sed (presentes no Git Bash / Linux / macOS)
# ============================================================
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
MANAGER_USER="${MANAGER_USER:-manager}"
MANAGER_PASS="${MANAGER_PASS:-Manager@123}"

echo "==> Gateway: $BASE_URL"
echo

# ---------- helpers ----------
req() { # req <method> <path> [data]
  local method="$1" path="$2" data="${3:-}"
  if [ -n "$data" ]; then
    curl -sS -X "$method" "$BASE_URL$path" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "$data"
  else
    curl -sS -X "$method" "$BASE_URL$path" -H "Authorization: Bearer $TOKEN"
  fi
}

json_field() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p" | head -1; }
json_num() { sed -n "s/.*\"$1\":\([0-9.]*\).*/\1/p" | head -1; }

TODAY="$(date +%F)"
# Portável: GNU date (Linux/Git Bash) vs BSD date (macOS)
if date -d '+90 days' >/dev/null 2>&1; then
  DUE_DATE="$(date -d '+90 days' +%F)"
else
  DUE_DATE="$(date -v+90d +%F)"
fi
PASS=0
FAIL=0

check() { # check <descrição> <condição>
  if [ "$2" = "true" ]; then
    PASS=$((PASS + 1))
    echo "  ✅ $1"
  else
    FAIL=$((FAIL + 1))
    echo "  ❌ $1"
  fi
}

# ---------- 1. Autenticação ----------
echo "1) Login ($MANAGER_USER)..."
LOGIN="$(curl -sS -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$MANAGER_USER\",\"password\":\"$MANAGER_PASS\"}")"
TOKEN="$(printf '%s' "$LOGIN" | json_field accessToken)"
check "JWT emitido" "$([ -n "$TOKEN" ] && echo true || echo false)"
ROLE="$(printf '%s' "$LOGIN" | json_field role)"
echo "     role: $ROLE"
[ -n "$TOKEN" ] || { echo "Falha no login. Resposta: $LOGIN"; exit 1; }

# ---------- 2. Tipos de recebíveis ----------
echo "2) Tipos de recebíveis..."
TYPES="$(req GET /api/v1/receivable-types)"
# Duplicata Mercantil = spread 1,5% a.m. (exemplo da spec: 100.000 / (1,02)^3 = 94.232,23)
TYPE_ID="$(printf '%s' "$TYPES" | sed -n 's/.*"id":"\([^"]*\)","name":"Duplicata Mercantil".*/\1/p' | head -1)"
check "Tipo 'Duplicata Mercantil' encontrado (id=$TYPE_ID)" "$([ -n "$TYPE_ID" ] && echo true || echo false)"

# ---------- 3. Taxa de câmbio (RF01 — manager) ----------
echo "3) Registro de taxa de câmbio USD→BRL..."
FX_RESP="$(req POST /api/v1/exchange-rates \
  "{\"fromCurrency\":\"USD\",\"toCurrency\":\"BRL\",\"rate\":5.4523,\"effectiveDate\":\"$TODAY\"}")"
FX_RATE="$(printf '%s' "$FX_RESP" | json_num rate)"
check "Taxa 5.4523 registrada (rate=$FX_RATE)" "$([ "$FX_RATE" = "5.4523" ] && echo true || echo false)"

# ---------- 4. Simulação de precificação (RF02 — exemplo da spec) ----------
echo "4) Simulação: 100.000 BRL em 90 dias (spread 1,5% + base 0,5% a.m.)..."
SIM="$(req POST /api/v1/receivables/price \
  "{\"faceValue\":100000,\"dueDate\":\"$DUE_DATE\",\"receivableTypeId\":\"$TYPE_ID\",\"currency\":\"BRL\",\"settlementCurrency\":\"BRL\",\"baseRate\":0.005}")"
PV="$(printf '%s' "$SIM" | json_num presentValue)"
check "Valor presente ≈ 94.232,23 (obtido: $PV)" \
  "$([ -n "$PV" ] && [ "$(printf '%.0f' "$PV")" = "94232" ] && echo true || echo false)"

# ---------- 5. Criação de recebível em lote (RF02) ----------
echo "5) Registro de lote de recebíveis..."
CEDENTE_CNPJ="11222333000181"
BATCH="$(req POST /api/v1/receivables \
  "{\"receivables\":[{\"cedenteDocument\":\"$CEDENTE_CNPJ\",\"receivableTypeId\":\"$TYPE_ID\",\"faceValue\":100000,\"dueDate\":\"$DUE_DATE\",\"currency\":\"BRL\"}]}")"
RECEIVABLE_ID="$(printf '%s' "$BATCH" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p' | head -1)"
check "Recebível criado (id=$RECEIVABLE_ID)" "$([ -n "$RECEIVABLE_ID" ] && echo true || echo false)"

# ---------- 6. Liquidação (RF03 — ACID + optimistic locking) ----------
echo "6) Liquidação em BRL..."
SETTLE="$(req POST "/api/v1/receivables/$RECEIVABLE_ID/settle" \
  "{\"settlementCurrency\":\"BRL\"}")"
TXN_ID="$(printf '%s' "$SETTLE" | json_field transactionId)"
TXN_STATUS="$(printf '%s' "$SETTLE" | json_field status)"
check "Transação COMPLETED (id=$TXN_ID, status=$TXN_STATUS)" \
  "$([ "$TXN_STATUS" = "COMPLETED" ] && echo true || echo false)"

# ---------- 7. Liquidação cross-currency (USD → BRL, RF04) ----------
echo "7) Recebível USD liquidado em BRL (conversão via currency-service)..."
BATCH_USD="$(req POST /api/v1/receivables \
  "{\"receivables\":[{\"cedenteDocument\":\"$CEDENTE_CNPJ\",\"receivableTypeId\":\"$TYPE_ID\",\"faceValue\":50000,\"dueDate\":\"$DUE_DATE\",\"currency\":\"USD\"}]}")"
RECEIVABLE_USD="$(printf '%s' "$BATCH_USD" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p' | head -1)"
SETTLE_USD="$(req POST "/api/v1/receivables/$RECEIVABLE_USD/settle" \
  "{\"settlementCurrency\":\"BRL\"}")"
USD_RATE="$(printf '%s' "$SETTLE_USD" | json_num exchangeRateApplied)"
PV_USD="$(printf '%s' "$SETTLE_USD" | json_num presentValue)"
PV_BRL="$(printf '%s' "$SETTLE_USD" | json_num presentValueInSettlementCurrency)"
check "Conversão cambial aplicada (taxa BRL/USD=$USD_RATE)" \
  "$([ -n "$USD_RATE" ] && [ "$USD_RATE" != "0" ] && echo true || echo false)"
check "PV em BRL maior que PV em USD ($PV_BRL > $PV_USD)" \
  "$([ -n "$PV_USD" ] && [ -n "$PV_BRL" ] && [ "$(printf '%.0f' "$PV_BRL")" -gt "$(printf '%.0f' "$PV_USD")" ] && echo true || echo false)"

# ---------- 8. Extrato (RF05 — CQRS via Kafka) ----------
echo "8) Extrato de liquidações (analytics)..."
sleep 3 # aguarda a projeção CQRS consumir o evento do Kafka
TXNS="$(req GET "/api/v1/transactions?startDate=$TODAY&endDate=$DUE_DATE")"
TOTAL_ELEMENTS="$(printf '%s' "$TXNS" | json_num totalElements)"
check "Extrato com liquidações (totalElements=$TOTAL_ELEMENTS)" \
  "$([ "${TOTAL_ELEMENTS:-0}" -ge 2 ] && echo true || echo false)"

SUMMARY="$(req GET "/api/v1/analytics/summary?startDate=$TODAY&endDate=$DUE_DATE")"
SUMMARY_TX="$(printf '%s' "$SUMMARY" | json_num totalTransactions)"
check "Resumo do dashboard (totalTransactions=$SUMMARY_TX)" \
  "$([ "${SUMMARY_TX:-0}" -ge 2 ] && echo true || echo false)"

# ---------- resultado ----------
echo
echo "=========================================="
echo " E2E SMOKE: $PASS passaram, $FAIL falharam"
echo "=========================================="
[ "$FAIL" -eq 0 ] || exit 1
