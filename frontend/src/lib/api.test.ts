import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError, setToken } from './api';

const originalFetch = globalThis.fetch;

function mockFetch(response: { status: number; body?: unknown; headers?: Record<string, string> }) {
  globalThis.fetch = vi.fn(async () => {
    return {
      ok: response.status >= 200 && response.status < 300,
      status: response.status,
      statusText: String(response.status),
      json: async () => response.body,
    } as Response;
  }) as unknown as typeof fetch;
}

describe('api client', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
    setToken(null);
  });

  it('anexa o token JWT e usa o prefixo /api/v1', async () => {
    setToken('jwt-token');
    mockFetch({ status: 200, body: { ok: true } });

    await api.get('/auth/me');

    const [url, init] = vi.mocked(globalThis.fetch).mock.calls[0];
    expect(String(url)).toBe('/api/v1/auth/me');
    const headers = init?.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer jwt-token');
  });

  it('serializa o corpo no POST', async () => {
    mockFetch({ status: 200, body: { ok: true } });

    await api.post('/auth/login', { username: 'admin', password: 'x' });

    const [, init] = vi.mocked(globalThis.fetch).mock.calls[0];
    expect(init?.method).toBe('POST');
    expect(init?.body).toBe(JSON.stringify({ username: 'admin', password: 'x' }));
  });

  it('lança ApiError com mensagem e correlationId do corpo', async () => {
    mockFetch({
      status: 400,
      body: { message: 'Requisição inválida.', correlationId: 'abc-123' },
    });

    const promise = api.get('/exchange-rates');
    await expect(promise).rejects.toBeInstanceOf(ApiError);
    await expect(promise).rejects.toMatchObject({ status: 400, correlationId: 'abc-123' });
  });

  it('lança ApiError com mensagem padrão quando o corpo não é JSON', async () => {
    mockFetch({ status: 401 });

    const promise = api.get('/transactions');
    await expect(promise).rejects.toMatchObject({ status: 401 });
  });
});
