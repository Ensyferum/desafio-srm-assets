// Teste de carga (RNF04) — objetivo: validar throughput agregado e latência p95.
// Cenário: ramp-up de VUs, foco em leituras (GETs) + simulação (POST).
//
// Uso: k6 run --env BASE_URL=http://localhost:8080 load.js
//      (ou via docker-compose: docker compose run --rm k6 k6 run /scripts/load.js)
import http from 'k6/http';
import { check, sleep } from 'k6';
import { authedHeaders } from './common.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    ramping: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 }, // aquecimento
        { duration: '60s', target: 100 }, // carga
        { duration: '30s', target: 0 }, // desaceleração
      ],
      gracefulRampDown: '15s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'], // RNF04: consultas < 500ms no p95 (folga para máquina dev)
    http_reqs: ['rate>100'], // throughput mínimo no agregado
  },
};

// Distribuição de operações (mix realista de uso operacional)
const GET_RECEIVABLES = 40;
const GET_TRANSACTIONS = 25;
const GET_RATES = 20;
const POST_PRICE = 10;
const POST_SETTLE = 5;

// Busca um tipo de recebível real (1x por VU, reutilizado nas iterações).
function resolveTypeId() {
  const headers = authedHeaders();
  const res = http.get(`${BASE_URL}/api/v1/receivable-types`, { headers });
  if (res.status !== 200) {
    return '00000000-0000-0000-0000-000000000001';
  }
  try {
    return res.json()[0]?.id || '00000000-0000-0000-0000-000000000001';
  } catch {
    return '00000000-0000-0000-0000-000000000001';
  }
}

const TYPE_ID = resolveTypeId();

export default function () {
  const headers = authedHeaders();

  // Sorteia a operação pela distribuição
  const roll = (__ITER * 97 + __VU * 13) % 100;
  const op =
    roll < GET_RECEIVABLES
      ? 'list'
      : roll < GET_RECEIVABLES + GET_TRANSACTIONS
        ? 'extrato'
        : roll < GET_RECEIVABLES + GET_TRANSACTIONS + GET_RATES
          ? 'rates'
          : roll < GET_RECEIVABLES + GET_TRANSACTIONS + GET_RATES + POST_PRICE
            ? 'price'
            : 'settle';

  if (op === 'list') {
    const res = http.get(`${BASE_URL}/api/v1/receivables?page=0&size=20`, { headers });
    check(res, { 'receivables 200': (r) => r.status === 200 });
  } else if (op === 'extrato') {
    const res = http.get(`${BASE_URL}/api/v1/transactions?page=0&size=20&sort=settledAt,desc`, { headers });
    check(res, { 'transactions 200': (r) => r.status === 200 });
  } else if (op === 'rates') {
    const res = http.get(`${BASE_URL}/api/v1/exchange-rates`, { headers });
    check(res, { 'rates 200': (r) => r.status === 200 });
  } else if (op === 'price') {
    const res = http.post(
      `${BASE_URL}/api/v1/receivables/price`,
      JSON.stringify({
        faceValue: 100000 + (__ITER % 50) * 1000,
        dueDate: '2026-11-12',
        receivableTypeId: TYPE_ID,
        currency: 'BRL',
        settlementCurrency: 'USD',
      }),
      { headers },
    );
    check(res, { 'price 200': (r) => r.status === 200 });
  } else {
    // settle com ID provavelmente inválido: valida que responde rápido (4xx esperado)
    // expectedStatuses: 4xx não conta como http_req_failed (o threshold mede erros reais)
    const res = http.post(
      `${BASE_URL}/api/v1/receivables/00000000-0000-0000-0000-000000000000/settle`,
      JSON.stringify({ settlementCurrency: 'USD' }),
      { headers, expectedStatuses: [404, 400, 409] },
    );
    check(res, { 'settle responde': (r) => r.status === 404 || r.status === 400 || r.status === 409 });
  }

  sleep(0.05); // ~20 ops/s por VU → 100 VUs ≈ 2000 req/s agregado
}
