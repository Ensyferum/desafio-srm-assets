import { useState, type FormEvent } from 'react';
import { CheckCircle2, ListOrdered, Search, Trash2, Plus } from 'lucide-react';
import { Badge } from '../components/Badge';
import { Button, Field, Select, TextInput } from '../components/Field';
import { Card } from '../components/Card';
import { ErrorAlert } from '../components/ErrorAlert';
import { Money } from '../components/Money';
import { Spinner } from '../components/Spinner';
import { api, ApiError } from '../lib/api';
import { addDaysISO, formatDate, formatRate } from '../lib/format';
import { useAsync } from '../lib/useAsync';
import type {
  CreateReceivablesBatchResponse,
  ReceivableResponse,
  ReceivableTypeResponse,
  SettleResponse,
} from '../lib/types';

interface BatchItem {
  cedenteId: string;
  receivableTypeId: string;
  faceValue: string;
  dueDate: string;
  currency: 'BRL' | 'USD';
}

function newItem(types: ReceivableTypeResponse[] | null): BatchItem {
  return {
    cedenteId: '11111111-1111-1111-1111-111111111111',
    receivableTypeId: types?.[0]?.id ?? '',
    faceValue: '',
    dueDate: addDaysISO(90),
    currency: 'BRL',
  };
}

/** Registro de lote de recebíveis (RF02) e liquidação (RF03). */
export function ReceivablesPage() {
  const types = useAsync<ReceivableTypeResponse[]>(() => api.get('/receivable-types'));
  const [items, setItems] = useState<BatchItem[]>(() => [newItem(null)]);
  const [batchResult, setBatchResult] = useState<CreateReceivablesBatchResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // Liquidação por ID
  const [searchId, setSearchId] = useState('');
  const [found, setFound] = useState<ReceivableResponse | null>(null);
  const [settleCurrency, setSettleCurrency] = useState<'BRL' | 'USD'>('BRL');
  const [settleResult, setSettleResult] = useState<SettleResponse | null>(null);
  const [settleError, setSettleError] = useState<string | null>(null);
  const [searching, setSearching] = useState(false);
  const [settling, setSettling] = useState(false);

  function updateItem(index: number, patch: Partial<BatchItem>) {
    setItems((list) => list.map((item, i) => (i === index ? { ...item, ...patch } : item)));
  }

  function handleAddItem() {
    setItems((list) => [...list, newItem(types.data)]);
  }

  function handleRemoveItem(index: number) {
    setItems((list) => (list.length > 1 ? list.filter((_, i) => i !== index) : list));
  }

  async function handleCreateBatch(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setBatchResult(null);
    setSaving(true);
    try {
      const response = await api.post<CreateReceivablesBatchResponse>('/receivables', {
        receivables: items.map((item) => ({
          cedenteId: item.cedenteId,
          receivableTypeId: item.receivableTypeId,
          faceValue: Number(item.faceValue),
          dueDate: item.dueDate,
          currency: item.currency,
        })),
      });
      setBatchResult(response);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Erro ao registrar lote.');
    } finally {
      setSaving(false);
    }
  }

  async function handleSearch(event: FormEvent) {
    event.preventDefault();
    if (!searchId.trim()) return;
    setFound(null);
    setSettleResult(null);
    setSettleError(null);
    setSearching(true);
    try {
      const response = await api.get<ReceivableResponse>(`/receivables/${searchId.trim()}`);
      setFound(response);
    } catch (err) {
      setSettleError(err instanceof ApiError ? err.message : 'Recebível não encontrado.');
    } finally {
      setSearching(false);
    }
  }

  async function handleSettle(event: FormEvent) {
    event.preventDefault();
    if (!found) return;
    setSettleResult(null);
    setSettleError(null);
    setSettling(true);
    try {
      const response = await api.post<SettleResponse>(`/receivables/${found.id}/settle`, {
        settlementCurrency: settleCurrency,
      });
      setSettleResult(response);
      setFound((f) => (f ? { ...f, status: 'SETTLED' } : f));
    } catch (err) {
      setSettleError(err instanceof ApiError ? err.message : 'Erro ao liquidar.');
    } finally {
      setSettling(false);
    }
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-xl font-bold tracking-tight text-slate-100">Recebíveis</h1>
        <p className="text-sm text-slate-400">Registro em lote (RF02) e liquidação (RF03/RF04)</p>
      </header>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-5">
        <Card
          title="Registrar lote de recebíveis"
          subtitle="Cria recebíveis com status PENDING"
          className="xl:col-span-3"
        >
          <form onSubmit={handleCreateBatch} className="space-y-4">
            {error && <ErrorAlert message={error} />}
            {batchResult && (
              <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-300">
                <p className="font-medium">
                  {batchResult.created} recebível(is) criado(s) com sucesso.
                </p>
              </div>
            )}

            {items.map((item, index) => (
              <div key={index} className="rounded-xl border border-slate-800 bg-slate-800/30 p-4">
                <div className="mb-3 flex items-center justify-between">
                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                    Recebível {index + 1}
                  </p>
                  <button
                    type="button"
                    onClick={() => handleRemoveItem(index)}
                    disabled={items.length === 1}
                    className="rounded-md p-1.5 text-slate-500 transition-colors hover:bg-rose-500/10 hover:text-rose-400 disabled:opacity-30"
                    aria-label="Remover recebível"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  <Field label="Cedente (ID)" required>
                    <TextInput
                      value={item.cedenteId}
                      onChange={(e) => updateItem(index, { cedenteId: e.target.value })}
                      placeholder="uuid do cedente"
                      className="font-mono text-xs"
                    />
                  </Field>
                  <Field label="Tipo" required>
                    <Select
                      value={item.receivableTypeId}
                      onChange={(e) => updateItem(index, { receivableTypeId: e.target.value })}
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
                  <Field label="Valor de face" required>
                    <TextInput
                      type="number"
                      min="0.01"
                      step="0.01"
                      value={item.faceValue}
                      onChange={(e) => updateItem(index, { faceValue: e.target.value })}
                      placeholder="50000"
                    />
                  </Field>
                  <Field label="Vencimento" required>
                    <TextInput
                      type="date"
                      value={item.dueDate}
                      onChange={(e) => updateItem(index, { dueDate: e.target.value })}
                    />
                  </Field>
                  <Field label="Moeda" required>
                    <Select
                      value={item.currency}
                      onChange={(e) => updateItem(index, { currency: e.target.value as 'BRL' | 'USD' })}
                    >
                      <option value="BRL">BRL</option>
                      <option value="USD">USD</option>
                    </Select>
                  </Field>
                </div>
              </div>
            ))}

            <div className="flex flex-wrap gap-3">
              <Button type="button" variant="secondary" onClick={handleAddItem}>
                <Plus className="h-4 w-4" />
                Adicionar recebível
              </Button>
              <Button type="submit" disabled={saving}>
                <ListOrdered className="h-4 w-4" />
                {saving ? 'Registrando…' : 'Registrar lote'}
              </Button>
            </div>
          </form>

          {batchResult && batchResult.receivables.length > 0 && (
            <div className="mt-5 border-t border-slate-800 pt-4">
              <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-400">
                IDs criados — use na liquidação ao lado
              </p>
              <ul className="space-y-1.5">
                {batchResult.receivables.map((r) => (
                  <li key={r.id} className="flex items-center justify-between gap-3 rounded-lg bg-slate-800/40 px-3 py-2">
                    <span className="font-mono text-xs text-slate-300">{r.id}</span>
                    <span className="flex items-center gap-2">
                      <Money value={r.faceValue} currency={r.currency} className="text-xs text-slate-400" />
                      <Badge status={r.status} />
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </Card>

        <div className="space-y-6 xl:col-span-2">
          <Card title="Liquidar recebível" subtitle="Busca por ID e liquidação ACID (RF03)">
            <form onSubmit={handleSearch} className="space-y-3">
              <Field label="ID do recebível" required>
                <div className="flex gap-2">
                  <TextInput
                    value={searchId}
                    onChange={(e) => setSearchId(e.target.value)}
                    placeholder="uuid"
                    className="font-mono text-xs"
                  />
                  <Button type="submit" variant="secondary" disabled={searching || !searchId.trim()}>
                    <Search className="h-4 w-4" />
                  </Button>
                </div>
              </Field>
            </form>

            {searching && <Spinner label="Buscando…" />}
            {settleError && <ErrorAlert message={settleError} />}

            {found && !settleError && (
              <form onSubmit={handleSettle} className="mt-4 space-y-3 rounded-xl border border-slate-800 bg-slate-800/30 p-4">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-semibold text-slate-200">{found.receivableTypeName}</p>
                  <Badge status={found.status} />
                </div>
                <dl className="space-y-1 text-sm">
                  <div className="flex justify-between">
                    <dt className="text-slate-400">Valor de face</dt>
                    <dd className="text-slate-200">
                      <Money value={found.faceValue} currency={found.currency} />
                    </dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-slate-400">Vencimento</dt>
                    <dd className="text-slate-200">{formatDate(found.dueDate)}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-slate-400">Moeda</dt>
                    <dd className="text-slate-200">{found.currency}</dd>
                  </div>
                </dl>
                {found.status === 'SETTLED' ? (
                  <p className="rounded-lg bg-emerald-500/10 px-3 py-2 text-xs text-emerald-300">
                    Este recebível já foi liquidado.
                  </p>
                ) : (
                  <>
                    <Field label="Moeda de liquidação" required>
                      <Select value={settleCurrency} onChange={(e) => setSettleCurrency(e.target.value as 'BRL' | 'USD')}>
                        <option value="BRL">BRL</option>
                        <option value="USD">USD</option>
                      </Select>
                    </Field>
                    <Button type="submit" variant="danger" disabled={settling} className="w-full">
                      <CheckCircle2 className="h-4 w-4" />
                      {settling ? 'Liquidando…' : 'Liquidar'}
                    </Button>
                  </>
                )}
              </form>
            )}

            {settleResult && (
              <div className="mt-4 rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-4 text-sm text-emerald-200">
                <p className="mb-2 flex items-center gap-2 font-semibold">
                  <CheckCircle2 className="h-4 w-4" />
                  Liquidação {settleResult.status}
                </p>
                <dl className="space-y-1">
                  <div className="flex justify-between">
                    <dt className="text-emerald-300/70">Valor presente</dt>
                    <dd>
                      <Money value={settleResult.presentValue} currency={found?.currency} />
                    </dd>
                  </div>
                  {(settleResult.exchangeRateApplied ?? 0) > 0 && (
                    <div className="flex justify-between">
                      <dt className="text-emerald-300/70">Câmbio aplicado</dt>
                      <dd className="tabular-nums">{settleResult.exchangeRateApplied!.toFixed(6)}</dd>
                    </div>
                  )}
                  <div className="flex justify-between">
                    <dt className="text-emerald-300/70">Pago em {settleResult.settlementCurrency}</dt>
                    <dd className="font-semibold text-emerald-300">
                      <Money value={settleResult.presentValueInSettlementCurrency} currency={settleResult.settlementCurrency} />
                    </dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-emerald-300/70">Transação</dt>
                    <dd className="font-mono text-xs">{settleResult.transactionId.slice(0, 8)}…</dd>
                  </div>
                </dl>
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
