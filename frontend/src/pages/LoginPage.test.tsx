import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { LoginPage } from './LoginPage';
import { AuthContext, type AuthContextValue } from '../lib/auth-context';

function renderLogin(auth: Partial<AuthContextValue> = {}) {
  const value: AuthContextValue = {
    user: null,
    token: null,
    login: vi.fn(async () => undefined),
    logout: vi.fn(),
    ...auth,
  };
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={value}>
        <LoginPage />
      </AuthContext.Provider>
    </MemoryRouter>,
  );
}

describe('LoginPage', () => {
  it('exibe título, campos e credenciais de demonstração', () => {
    renderLogin();
    expect(screen.getByRole('heading', { name: /srm credit engine/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/usuário/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/senha/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /entrar/i })).toBeInTheDocument();
    expect(screen.getByText('manager')).toBeInTheDocument();
    expect(screen.getByText('admin')).toBeInTheDocument();
  });

  it('preenche credenciais ao clicar em um usuário de demonstração', () => {
    renderLogin();
    const managerCard = screen.getByRole('button', { name: /manager/i });
    fireEvent.click(managerCard);
    const usernameInput = screen.getByLabelText(/usuário/i) as HTMLInputElement;
    const passwordInput = screen.getByLabelText(/senha/i) as HTMLInputElement;
    expect(usernameInput.value).toBe('manager');
    expect(passwordInput.value).toBe('Manager@123');
  });
});
