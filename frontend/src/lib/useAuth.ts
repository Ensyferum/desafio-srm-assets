import { useContext } from 'react';
import { AuthContext, type AuthContextValue } from './auth-context';
import type { Role } from './types';

/** Hook de autenticação (deve ser usado dentro de <AuthProvider>). */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth deve ser usado dentro de <AuthProvider>');
  }
  return ctx;
}

export function useRole(): Role | null {
  return useAuth().user?.role ?? null;
}
