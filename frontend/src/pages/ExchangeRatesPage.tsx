import { useMemo, useState, type FormEvent } from 'react';
import { ArrowRightLeft, Plus } from 'lucide-react';
import { Button, Field, Select, TextInput } from '../components/Field';
import { Card } from '../components/Card';
import { ErrorAlert } from '../components/ErrorAlert';
import { Spinner } from '../components/Spinner';
import { api, ApiError } from '../lib/api';
import { useRole } from '../lib/useAuth';
import { formatDate, formatNumber, todayISO } from '../lib/format';
import { useToast } from '../lib/toast';
import { useAsync } from '../lib/useAsync';
import type { ExchangeRateResponse } from '../lib/types';

/** Gestão de taxas de câmbio BRL/USD (RF01). */
export function ExchangeRatesPage() {
  const role = useRole();
  const canManage = role === 'MANAGER' || role === 'ADMIN';
  const rates = useAsync<ExchangeRateResponse[]>(() => api.get('/exchange-rates'));
  const [from, setFrom] = useState('USD');
  const [to, setTo] = useState('BRL');
  const [rate, setRate] = useState('');
  const [effectiveDate, setEffectiveDate] = useState(todayISO());
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const { push } = useToast();

  // Agrupa por par e ordena do mais recente ao mais antigo
  const grouped = useMemo(() => {
    const map = new Map<string, ExchangeRateResponse[]>();
    for (const r of rates.data ?? []) {
      const key = `${r.fromCurrency}→${r.toCurrency}`;
      const list = map.get(key) ?? [];
      list.push(r);
      map.set(key, list);
    }
    return Array.from(map.entries()).sort((a, b) => a[0].localeCompare(b[0]));
  }, [rates.data]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const rateValue = Number(rate);
    if (!rateValue || rateValue <= 0) {
      setError('Informe uma taxa maior que zero.');
      return;
    }
    setError(null);
    setSaving(true);
    try {
      await api.post('/exchange-rates', {
        fromCurrency: from,
        toCurrency: to,
        rate: rateValue,
        effectiveDate,
      });
      push('success', `Taxa ${from}→${to} registrada para ${formatDate(effectiveDate)}.`);
      setRate('');
      void rates.reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Erro ao registrar taxa.');
      push('error', err instanceof ApiError ? err.message : 'Erro ao registrar taxa.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-xl font-bold tracking-tight text-slate-100">Taxas de câmbio</h1>
        <p className="text-sm text-slate-400">Gestão de taxas BRL/USD · RF01</p>
      </header>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-5">
        <Card
          title="Histórico por par"
          subtitle="Taxas vigentes e histórico registrado"
          className="xl:col-span-3"
          padding={false}
        >
          {rates.loading ? (
            <Spinner label="Carregando…" />
          ) : rates.error ? (
            <div className="p-5">
              <ErrorAlert message={rates.error} />
            </div>
          ) : grouped.length === 0 ? (
            <p className="py-10 text-center text-sm text-slate-500">
              Nenhuma taxa cadastrada ainda. {canManage ? 'Registre a primeira ao lado.' : ''}
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-800 text-xs uppercase tracking-wider text-slate-500">
                    <th className="px-5 py-3 font-medium">Par</th>
                    <th className="px-5 py-3 font-medium">Taxa</th>
                    <th className="px-5 py-3 font-medium">Vigência</th>
                  </tr>
                </thead>
                <tbody>
                  {grouped.map(([pair, list]) => (
                    <tr key={pair} className="border-b border-slate-800/60 last:border-0">
                      <td className="px-5 py-3">
                        <span className="inline-flex items-center gap-1.5 font-medium text-slate-200">
                          {list[0].fromCurrency}
                          <ArrowRightLeft className="h-3.5 w-3.5 text-brand-400" />
                          {list[0].toCurrency}
                        </span>
                      </td>
                      <td className="px-5 py-3">
                        <span className="tabular-nums font-semibold text-slate-100">
                          {formatNumber(list[0].rate, 6)}
                        </span>
                        <span className="ml-2 text-xs text-slate-500">({list.length} registro{list.length > 1 ? 's' : ''})</span>
                      </td>
                      <td className="px-5 py-3 text-slate-300">{formatDate(list[0].effectiveDate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        <Card
          title="Registrar taxa"
          subtitle={canManage ? 'Cria ou atualiza a taxa do par na data' : 'Apenas MANAGER/ADMIN podem registrar'}
          className="xl:col-span-2"
        >
          {!canManage ? (
            <p className="py-6 text-center text-sm text-slate-500">
              Sua role ({role}) não permite registrar taxas. Solicite a um gerente.
            </p>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && <ErrorAlert message={error} />}
              <div className="grid grid-cols-2 gap-4">
                <Field label="De" required>
                  <Select value={from} onChange={(e) => setFrom(e.target.value)}>
                    <option value="USD">USD</option>
                    <option value="BRL">BRL</option>
                  </Select>
                </Field>
                <Field label="Para" required>
                  <Select value={to} onChange={(e) => setTo(e.target.value)}>
                    <option value="BRL">BRL</option>
                    <option value="USD">USD</option>
                  </Select>
                </Field>
              </div>
              <Field label="Taxa" required hint="1 USD = N BRL">
                <TextInput
                  type="number"
                  min="0.0000000001"
                  step="0.000001"
                  value={rate}
                  onChange={(e) => setRate(e.target.value)}
                  placeholder="5.80"
                />
              </Field>
              <Field label="Data de vigência" required>
                <TextInput type="date" value={effectiveDate} onChange={(e) => setEffectiveDate(e.target.value)} />
              </Field>
              <Button type="submit" disabled={saving} className="w-full">
                <Plus className="h-4 w-4" />
                {saving ? 'Registrando…' : 'Registrar taxa'}
              </Button>
            </form>
          )}
        </Card>
      </div>
    </div>
  );
}
