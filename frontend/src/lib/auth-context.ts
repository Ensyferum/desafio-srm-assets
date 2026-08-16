import { createContext } from 'react';
import type { Role } from './types';

export interface StoredAuth {
  accessToken: string;
  username: string;
  fullName: string;
  role: Role;
}

export interface AuthContextValue {
  user: Omit<StoredAuth, 'accessToken'> | null;
  token: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
