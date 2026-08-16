import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { KeyRound, LogIn, ShieldCheck, User } from 'lucide-react';
import { Button, Field, TextInput } from '../components/Field';
import { ErrorAlert } from '../components/ErrorAlert';
import { useAuth } from '../lib/useAuth';

const demoUsers = [
  { username: 'admin', password: 'Admin@123', role: 'ADMIN' },
  { username: 'manager', password: 'Manager@123', role: 'MANAGER' },
  { username: 'operator', password: 'Operator@123', role: 'OPERATOR' },
];

/** Página de autenticação. */
export function LoginPage() {
  const { token, login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (token) {
    return <Navigate to="/" replace />;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username.trim(), password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha no login.');
    } finally {
      setSubmitting(false);
    }
  }

  function fillDemo(user: (typeof demoUsers)[number]) {
    setUsername(user.username);
    setPassword(user.password);
    setError(null);
  }

  return (
    <div className="flex min-h-full items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-600 shadow-xl shadow-brand-900/50">
            <ShieldCheck className="h-7 w-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-100">SRM Credit Engine</h1>
          <p className="mt-1 text-sm text-slate-400">
            Plataforma de cessão de crédito multimoedas (BRL/USD)
          </p>
        </div>

        <form
          onSubmit={handleSubmit}
          className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6 shadow-2xl shadow-black/40 backdrop-blur"
        >
          {error && <div className="mb-4">{error && <ErrorAlert message={error} />}</div>}

          <div className="space-y-4">
            <Field label="Usuário" required>
              <div className="relative">
                <User className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
                <TextInput
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="nome de usuário"
                  autoComplete="username"
                  autoFocus
                  className="pl-9"
                />
              </div>
            </Field>

            <Field label="Senha" required>
              <div className="relative">
                <KeyRound className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
                <TextInput
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  autoComplete="current-password"
                  className="pl-9"
                />
              </div>
            </Field>
          </div>

          <Button type="submit" disabled={submitting || !username || !password} className="mt-6 w-full">
            {submitting ? 'Entrando…' : 'Entrar'}
            {!submitting && <LogIn className="h-4 w-4" />}
          </Button>

          <div className="mt-6 border-t border-slate-800 pt-4">
            <p className="mb-2 text-center text-[11px] uppercase tracking-wider text-slate-500">
              Credenciais de demonstração
            </p>
            <div className="grid grid-cols-3 gap-2">
              {demoUsers.map((user) => (
                <button
                  key={user.username}
                  type="button"
                  onClick={() => fillDemo(user)}
                  className="rounded-lg border border-slate-800 bg-slate-800/40 px-2 py-2 text-center transition-colors hover:border-brand-500/40 hover:bg-slate-800"
                >
                  <span className="block text-xs font-semibold text-slate-200">{user.username}</span>
                  <span className="block text-[10px] uppercase text-slate-500">{user.role}</span>
                </button>
              ))}
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
