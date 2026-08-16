import { useEffect, useState, type FormEvent } from 'react';
import { ChevronLeft, ChevronRight, Filter } from 'lucide-react';
import { Badge } from '../components/Badge';
import { Button, Field, Select, TextInput } from '../components/Field';
import { Card } from '../components/Card';
import { ErrorAlert } from '../components/ErrorAlert';
import { Money } from '../components/Money';
import { Spinner } from '../components/Spinner';
import { api } from '../lib/api';
import { formatDateTime, formatNumber, todayISO } from '../lib/format';
import { useAsync } from '../lib/useAsync';
import type { PageResponse, TransactionSummary } from '../lib/types';

const PAGE_SIZE = 10;

/** Extrato de liquidações com filtros e paginação server-side (RF05). */
export function ExtratoPage() {
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [currency, setCurrency] = useState('');
  const [page, setPage] = useState(0);
  // Filtros efetivamente aplicados (draft é separado para evitar reload por tecla)
  const [applied, setApplied] = useState({ startDate: '', endDate: '', currency: '' });

  const buildUrl = () => {
    const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE), sort: 'settledAt,desc' });
    if (applied.startDate) params.set('startDate', applied.startDate);
    if (applied.endDate) params.set('endDate', applied.endDate);
    if (applied.currency) params.set('currency', applied.currency);
    return `/transactions?${params.toString()}`;
  };

  const result = useAsync<PageResponse<TransactionSummary>>(() => api.get(buildUrl()), [page, applied]);

  useEffect(() => {
    setPage(0);
  }, [applied]);

  function handleApply(event: FormEvent) {
    event.preventDefault();
    setApplied({ startDate, endDate, currency });
    setPage(0);
  }

  const data = result.data;

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-xl font-bold tracking-tight text-slate-100">Extrato de liquidações</h1>
        <p className="text-sm text-slate-400">Consulta analítica com paginação server-side · RF05</p>
      </header>

      <Card title="Filtros" subtitle="Período e moeda de liquidação">
        <form onSubmit={handleApply} className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Field label="De">
            <TextInput type="date" max={endDate || undefined} value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          </Field>
          <Field label="Até">
            <TextInput type="date" min={startDate || undefined} max={todayISO()} value={endDate} onChange={(e) => setEndDate(e.target.value)} />
          </Field>
          <Field label="Moeda de liquidação">
            <Select value={currency} onChange={(e) => setCurrency(e.target.value)}>
              <option value="">Todas</option>
              <option value="BRL">BRL</option>
              <option value="USD">USD</option>
            </Select>
          </Field>
          <div className="flex items-end">
            <Button type="submit" className="w-full">
              <Filter className="h-4 w-4" />
              Aplicar filtros
            </Button>
          </div>
        </form>
      </Card>

      {result.error && <ErrorAlert message={result.error} />}

      <Card
        title="Liquidações"
        subtitle={data ? `${data.totalElements} registro(s) · página ${data.page + 1} de ${Math.max(data.totalPages, 1)}` : ' '}
        padding={false}
        actions={
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="secondary"
              disabled={!data || data.first}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="px-2.5"
              aria-label="Página anterior"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="text-xs tabular-nums text-slate-400">
              {data ? `${data.page + 1}/${Math.max(data.totalPages, 1)}` : '—'}
            </span>
            <Button
              type="button"
              variant="secondary"
              disabled={!data || data.last}
              onClick={() => setPage((p) => p + 1)}
              className="px-2.5"
              aria-label="Próxima página"
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        }
      >
        {result.loading ? (
          <Spinner label="Carregando…" />
        ) : !data || data.empty ? (
          <p className="py-10 text-center text-sm text-slate-500">
            Nenhuma liquidação encontrada para os filtros selecionados.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-800 text-xs uppercase tracking-wider text-slate-500">
                  <th className="px-5 py-3 font-medium">Data</th>
                  <th className="px-5 py-3 font-medium">Valor face</th>
                  <th className="px-5 py-3 font-medium">Valor presente</th>
                  <th className="px-5 py-3 font-medium">Desconto</th>
                  <th className="px-5 py-3 font-medium">Câmbio</th>
                  <th className="px-5 py-3 font-medium">Liquidação</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((tx) => (
                  <tr key={tx.transactionId} className="border-b border-slate-800/60 last:border-0 hover:bg-slate-800/30">
                    <td className="whitespace-nowrap px-5 py-3 text-slate-300">{formatDateTime(tx.settledAt)}</td>
                    <td className="px-5 py-3 text-slate-300">
                      <Money value={tx.faceValue} currency={tx.currency} />
                    </td>
                    <td className="px-5 py-3 font-medium text-slate-100">
                      <Money value={tx.presentValue} currency={tx.currency} />
                    </td>
                    <td className="px-5 py-3 text-amber-400">
                      <Money value={tx.discountValue} currency={tx.currency} />
                    </td>
                    <td className="px-5 py-3 text-slate-400">
                      {(tx.exchangeRateApplied ?? 0) > 0 ? (
                        <span className="tabular-nums">{formatNumber(tx.exchangeRateApplied, 6)}</span>
                      ) : (
                        '1.000000'
                      )}
                    </td>
                    <td className="px-5 py-3 text-slate-300">
                      {tx.currency} → {tx.settlementCurrency}
                    </td>
                    <td className="px-5 py-3">
                      <Badge status={tx.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
