import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { Calculator, Info } from 'lucide-react';
import { Button, Field, Select, TextInput } from '../components/Field';
import { Card } from '../components/Card';
import { ErrorAlert } from '../components/ErrorAlert';
import { Money } from '../components/Money';
import { Spinner } from '../components/Spinner';
import { api, ApiError } from '../lib/api';
import { addDaysISO, formatDate, formatNumber, formatRate, todayISO } from '../lib/format';
import { useAsync } from '../lib/useAsync';
import type { PriceSimulationResponse, ReceivableTypeResponse } from '../lib/types';

interface SimForm {
  faceValue: string;
  dueDate: string;
  receivableTypeId: string;
  currency: 'BRL' | 'USD';
  settlementCurrency: 'BRL' | 'USD';
  baseRate: string;
}

const emptyForm: SimForm = {
  faceValue: '100000',
  dueDate: addDaysISO(90),
  receivableTypeId: '',
  currency: 'BRL',
  settlementCurrency: 'BRL',
  baseRate: '0.005',
};

/** Simulação de precificação em tempo real (RF02). */
export function SimulationPage() {
  const types = useAsync<ReceivableTypeResponse[]>(() => api.get('/receivable-types'));
  const [form, setForm] = useState<SimForm>(emptyForm);
  const [result, setResult] = useState<PriceSimulationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Pré-seleciona o primeiro tipo quando carrega
  useEffect(() => {
    if (types.data && types.data.length > 0 && !form.receivableTypeId) {
      setForm((f) => ({ ...f, receivableTypeId: types.data![0].id }));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [types.data]);

  const runSimulation = useCallback(async (values: SimForm) => {
    const faceValue = Number(values.faceValue);
    if (!values.receivableTypeId || !values.dueDate || !Number.isFinite(faceValue) || faceValue <= 0) {
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const response = await api.post<PriceSimulationResponse>('/receivables/price', {
        faceValue,
        dueDate: values.dueDate,
        receivableTypeId: values.receivableTypeId,
        currency: values.currency,
        settlementCurrency: values.settlementCurrency,
        baseRate: values.baseRate ? Number(values.baseRate) : undefined,
      });
      setResult(response);
    } catch (err) {
      setResult(null);
      setError(err instanceof ApiError ? err.message : 'Erro ao simular.');
    } finally {
      setLoading(false);
    }
  }, []);

  // Simulação em tempo real com debounce de 500ms
  useEffect(() => {
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => {
      void runSimulation(form);
    }, 500);
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [form, runSimulation]);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (timerRef.current) clearTimeout(timerRef.current);
    void runSimulation(form);
  }

  function update<K extends keyof SimForm>(key: K, value: SimForm[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-xl font-bold tracking-tight text-slate-100">Simulação de precificação</h1>
        <p className="text-sm text-slate-400">
          Valor presente = Valor face / (1 + taxa base + spread)^prazo · RF02
        </p>
      </header>

      {error && <ErrorAlert message={error} />}

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-5">
        <Card title="Parâmetros" subtitle="A simulação roda automaticamente ao alterar" className="xl:col-span-3">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Valor de face" required>
                <TextInput
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={form.faceValue}
                  onChange={(e) => update('faceValue', e.target.value)}
                  placeholder="100000"
                />
              </Field>
              <Field label="Vencimento" required>
                <TextInput
                  type="date"
                  min={todayISO()}
                  value={form.dueDate}
                  onChange={(e) => update('dueDate', e.target.value)}
                />
              </Field>
              <Field label="Tipo de recebível" required>
                <Select
                  value={form.receivableTypeId}
                  onChange={(e) => update('receivableTypeId', e.target.value)}
                  disabled={types.loading}
                >
                  {!types.data && <option value="">Carregando…</option>}
                  {types.data?.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name} · {formatRate(t.spreadMonthly)}
                    </option>
                  ))}
                </Select>
              </Field>
              <Field label="Taxa base (a.m.)" hint="Ex.: 0.005 = 0,50% a.m.">
                <TextInput
                  type="number"
                  min="0"
                  step="0.001"
                  value={form.baseRate}
                  onChange={(e) => update('baseRate', e.target.value)}
                  placeholder="0.005"
                />
              </Field>
              <Field label="Moeda do ativo" required>
                <Select value={form.currency} onChange={(e) => update('currency', e.target.value as 'BRL' | 'USD')}>
                  <option value="BRL">BRL — Real</option>
                  <option value="USD">USD — Dólar</option>
                </Select>
              </Field>
              <Field label="Moeda de liquidação" required>
                <Select
                  value={form.settlementCurrency}
                  onChange={(e) => update('settlementCurrency', e.target.value as 'BRL' | 'USD')}
                >
                  <option value="BRL">BRL — Real</option>
                  <option value="USD">USD — Dólar</option>
                </Select>
              </Field>
            </div>
            <Button type="submit" disabled={loading} className="sm:w-auto">
              <Calculator className="h-4 w-4" />
              Simular agora
            </Button>
          </form>
        </Card>

        <div className="xl:col-span-2">
          <Card title="Resultado" subtitle={result ? 'Cálculo vigente' : 'Aguardando parâmetros válidos'}>
            {loading ? (
              <Spinner label="Calculando…" />
            ) : !result ? (
              <div className="flex flex-col items-center gap-2 py-8 text-center text-slate-500">
                <Info className="h-8 w-8 text-slate-600" />
                <p className="text-sm">
                  Preencha os parâmetros e o valor presente será calculado em tempo real.
                </p>
              </div>
            ) : (
              <dl className="space-y-3 text-sm">
                <div className="flex justify-between border-b border-slate-800 pb-2">
                  <dt className="text-slate-400">Valor de face</dt>
                  <dd className="font-medium text-slate-200">
                    <Money value={result.faceValue} currency={result.currency} />
                  </dd>
                </div>
                <div className="flex justify-between border-b border-slate-800 pb-2">
                  <dt className="text-slate-400">Valor presente</dt>
                  <dd className="text-lg font-bold text-brand-400">
                    <Money value={result.presentValue} currency={result.currency} />
                  </dd>
                </div>
                <div className="flex justify-between border-b border-slate-800 pb-2">
                  <dt className="text-slate-400">Desconto (deságio)</dt>
                  <dd className="font-medium text-amber-400">
                    <Money value={result.discountValue} currency={result.currency} />
                  </dd>
                </div>
                <div className="flex justify-between border-b border-slate-800 pb-2">
                  <dt className="text-slate-400">Spread aplicado</dt>
                  <dd className="font-medium text-slate-200">{formatRate(result.spreadApplied)}</dd>
                </div>
                <div className="flex justify-between border-b border-slate-800 pb-2">
                  <dt className="text-slate-400">Taxa base</dt>
                  <dd className="font-medium text-slate-200">{formatRate(result.baseRate)}</dd>
                </div>
                <div className="flex justify-between border-b border-slate-800 pb-2">
                  <dt className="text-slate-400">Prazo</dt>
                  <dd className="font-medium text-slate-200">{formatNumber(result.termMonths, 1)} meses</dd>
                </div>
                {(result.exchangeRateApplied ?? 0) > 0 && (
                  <>
                    <div className="flex justify-between border-b border-slate-800 pb-2">
                      <dt className="text-slate-400">Taxa de câmbio aplicada</dt>
                      <dd className="font-medium text-slate-200">{formatNumber(result.exchangeRateApplied, 6)}</dd>
                    </div>
                    <div className="flex justify-between pb-2">
                      <dt className="text-slate-400">
                        Valor em {result.settlementCurrency} (conversão)
                      </dt>
                      <dd className="font-semibold text-sky-400">
                        <Money value={result.presentValueInSettlementCurrency} currency={result.settlementCurrency} />
                      </dd>
                    </div>
                  </>
                )}
                <p className="pt-1 text-xs text-slate-500">
                  Tipo: {result.receivableTypeName} · vencimento {formatDate(form.dueDate)}
                </p>
              </dl>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
