// Smoke test — valida o fluxo completo com carga mínima (1 VU, poucas iterações).
// Executa em segundos e serve de sanidade antes do teste de carga.
//
// Uso: k6 run --env BASE_URL=http://localhost:8080 smoke.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { authedHeaders } from './common.js';

export const options = {
  vus: 1,
  iterations: 5,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const headers = authedHeaders();

  // Lista de recebíveis (paginada)
  const list = http.get(`${BASE}/api/v1/receivables?page=0&size=10`, { headers });
  check(list, { 'receivables 200': (r) => r.status === 200 });

  // Taxas de câmbio
  const rates = http.get(`${BASE}/api/v1/exchange-rates`, { headers });
  check(rates, { 'exchange-rates 200': (r) => r.status === 200 });

  // Extrato de liquidações
  const extrato = http.get(`${BASE}/api/v1/transactions?page=0&size=10`, { headers });
  check(extrato, { 'transactions 200': (r) => r.status === 200 });

  // Tipos de recebível (para simular com um id real)
  const types = http.get(`${BASE}/api/v1/receivable-types`, { headers });
  check(types, { 'receivable-types 200': (r) => r.status === 200 });
  const typeId = types.json()[0]?.id || '00000000-0000-0000-0000-000000000001';

  // Simulação de precificação (POST) com tipo real
  const price = http.post(
    `${BASE}/api/v1/receivables/price`,
    JSON.stringify({
      faceValue: 100000,
      dueDate: '2026-11-12',
      receivableTypeId: typeId,
      currency: 'BRL',
      settlementCurrency: 'USD',
    }),
    { headers },
  );
  check(price, { 'price 200': (r) => r.status === 200 });

  sleep(0.2);
}
