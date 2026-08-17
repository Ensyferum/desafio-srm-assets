import { useEffect, useState, type FormEvent } from 'react';
import { CheckCircle2, Search } from 'lucide-react';
import { Button, Field, Select, TextInput } from '../../components/Field';
import { Card } from '../../components/Card';
import { ErrorAlert } from '../../components/ErrorAlert';
import { Money } from '../../components/Money';
import { Spinner } from '../../components/Spinner';
import { Badge } from '../../components/Badge';
import { api, ApiError } from '../../lib/api';
import { formatDate } from '../../lib/format';
import { useToast } from '../../lib/toast';
import type { ReceivableResponse, SettleResponse } from '../../lib/types';

interface SettleTabProps {
  /** ID vindo da lista de recebíveis (busca automática ao chegar). */
  pendingId: string;
  onConsumed: () => void;
}

/** Liquidação por ID com busca automática a partir da lista (RF03). */
export function SettleTab({ pendingId, onConsumed }: SettleTabProps) {
  const [searchId, setSearchId] = useState('');
  const [found, setFound] = useState<ReceivableResponse | null>(null);
  const [settleCurrency, setSettleCurrency] = useState<'BRL' | 'USD'>('BRL');
  const [settleResult, setSettleResult] = useState<SettleResponse | null>(null);
  const [settleError, setSettleError] = useState<string | null>(null);
  const [searching, setSearching] = useState(false);
  const [settling, setSettling] = useState(false);
  const { push } = useToast();

  async function runSearch(id: string) {
    setFound(null);
    setSettleResult(null);
    setSettleError(null);
    setSearching(true);
    try {
      const response = await api.get<ReceivableResponse>(`/receivables/${id}`);
      setFound(response);
    } catch (err) {
      setSettleError(err instanceof ApiError ? err.message : 'Recebível não encontrado.');
    } finally {
      setSearching(false);
    }
  }

  // Busca automática quando o ID chega da lista de recebíveis
  useEffect(() => {
    if (!pendingId) return;
    setSearchId(pendingId);
    void runSearch(pendingId);
    onConsumed();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pendingId]);

  function handleSearch(event: FormEvent) {
    event.preventDefault();
    if (!searchId.trim()) return;
    void runSearch(searchId.trim());
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
      push('success', 'Liquidação concluída com sucesso.');
    } catch (err) {
      setSettleError(err instanceof ApiError ? err.message : 'Erro ao liquidar.');
      push('error', err instanceof ApiError ? err.message : 'Erro ao liquidar.');
    } finally {
      setSettling(false);
    }
  }

  return (
    <Card title="Liquidar recebível" subtitle="Busca por ID e liquidação ACID · RF03">
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
  );
}
