import type { ErrorResponseBody } from './types';

/** Erro de API com corpo padronizado (correlationId, fieldErrors). */
export class ApiError extends Error {
  readonly status: number;
  readonly correlationId?: string;
  readonly errorId?: string;
  readonly fieldErrors?: Record<string, string>;

  constructor(status: number, message: string, body?: Partial<ErrorResponseBody>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.correlationId = body?.correlationId;
    this.errorId = body?.errorId;
    this.fieldErrors = body?.fieldErrors;
  }
}

const TOKEN_KEY = 'srm.accessToken';

/** Evento disparado quando a API responde 401 (sessão expirada/inválida). */
export const UNAUTHORIZED_EVENT = 'srm:unauthorized';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null): void {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers = new Headers(init.headers);
  if (!headers.has('Content-Type') && init.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`/api/v1${path}`, { ...init, headers });

  if (!response.ok) {
    let body: Partial<ErrorResponseBody> | undefined;
    try {
      body = (await response.json()) as Partial<ErrorResponseBody>;
    } catch {
      body = undefined;
    }
    const message =
      body?.message ??
      (response.status === 401
        ? 'Sessão expirada. Faça login novamente.'
        : `Erro ${response.status} — ${response.statusText}`);
    if (response.status === 401) {
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
    throw new ApiError(response.status, message, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};
