// Helpers compartilhados dos testes de carga (RNF04).
// Uso: k6 run --env BASE_URL=http://localhost:8080 smoke.js
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USERNAME = __ENV.USERNAME || 'manager';
const PASSWORD = __ENV.PASSWORD || 'Manager@123';

// Login e cache do token (1 login por VU; o token JWT dura 8h no dev).
let cachedToken = '';

export function loginToken() {
  if (cachedToken) return cachedToken;
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(res, { 'login 200': (r) => r.status === 200 });
  cachedToken = res.json('accessToken');
  return cachedToken;
}

export function authedHeaders(extra = {}) {
  return {
    Authorization: `Bearer ${loginToken()}`,
    'Content-Type': 'application/json',
    ...extra,
  };
}

export { BASE_URL };
