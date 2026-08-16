import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, setToken, UNAUTHORIZED_EVENT } from './api';
import { AuthContext, type StoredAuth } from './auth-context';
import type { LoginResponse } from './types';

const STORAGE_KEY = 'srm.auth';

function readStored(): StoredAuth | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as StoredAuth;
    if (!parsed.accessToken || !parsed.username) return null;
    return parsed;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [stored, setStored] = useState<StoredAuth | null>(() => readStored());

  const login = useCallback(async (username: string, password: string) => {
    const response = await api.post<LoginResponse>('/auth/login', { username, password });
    const next: StoredAuth = {
      accessToken: response.accessToken,
      username: response.username,
      fullName: response.fullName,
      role: response.role,
    };
    setToken(response.accessToken);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    setStored(next);
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    localStorage.removeItem(STORAGE_KEY);
    setStored(null);
  }, []);

  // Sessão expirada: a API respondeu 401 — encerra a sessão automaticamente.
  useEffect(() => {
    const onUnauthorized = () => logout();
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, [logout]);

  const contextValue = useMemo(
    () => ({
      user: stored ? { username: stored.username, fullName: stored.fullName, role: stored.role } : null,
      token: stored?.accessToken ?? null,
      login,
      logout,
    }),
    [stored, login, logout],
  );

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
}
