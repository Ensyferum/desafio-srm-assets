import { useEffect, useState, type FormEvent } from 'react';
import { CheckCircle2, Filter, Inbox } from 'lucide-react';
import { Button, Field, Select } from '../../components/Field';
import { Card } from '../../components/Card';
import { ErrorAlert } from '../../components/ErrorAlert';
import { Money } from '../../components/Money';
import { Spinner } from '../../components/Spinner';
import { Badge } from '../../components/Badge';
import { Pagination } from '../../components/Pagination';
import { api } from '../../lib/api';
import { formatDate } from '../../lib/format';
import { useAsync } from '../../lib/useAsync';
import type { PageResponse, ReceivableResponse } from '../../lib/types';

const PAGE_SIZE = 10;
const STATUS_OPTIONS = ['PENDING', 'PRICED', 'SETTLED', 'CANCELLED'] as const;

interface ReceivablesListTabProps {
  onSettle: (id: string) => void;
}

/** Lista de recebíveis com filtros e paginação server-side (RF02/RF03). */
export function ReceivablesListTab({ onSettle }: ReceivablesListTabProps) {
  const [status, setStatus] = useState('');
  const [currency, setCurrency] = useState('');
  const [applied, setApplied] = useState({ status: '', currency: '' });
  const [page, setPage] = useState(0);

  const buildUrl = () => {
    const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE), sort: 'createdAt,desc' });
    if (applied.status) params.set('status', applied.status);
    if (applied.currency) params.set('currency', applied.currency);
    return `/receivables?${params.toString()}`;
  };

  const result = useAsync<PageResponse<ReceivableResponse>>(() => api.get(buildUrl()), [page, applied]);

  useEffect(() => {
    setPage(0);
  }, [applied]);

  function handleApply(event: FormEvent) {
    event.preventDefault();
    setApplied({ status, currency });
    setPage(0);
  }

  const data = result.data;

  return (
    <Card
      title="Recebíveis registrados"
      subtitle={data ? `${data.totalElements} recebível(is) · página ${data.page + 1} de ${Math.max(data.totalPages, 1)}` : ' '}
      padding={false}
      actions={data && <Pagination page={data.page} totalPages={data.totalPages} first={data.first} last={data.last} onChange={setPage} />}
    >
      <div className="border-b border-slate-800 p-5">
        <form onSubmit={handleApply} className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Field label="Status">
            <Select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">Todos</option>
              {STATUS_OPTIONS.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Moeda">
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
      </div>

      {result.error && (
        <div className="p-5">
          <ErrorAlert message={result.error} />
        </div>
      )}

      {result.loading ? (
        <Spinner label="Carregando…" />
      ) : !data || data.empty ? (
        <div className="flex flex-col items-center gap-2 py-12 text-center text-slate-500">
          <Inbox className="h-8 w-8 text-slate-600" />
          <p className="text-sm">Nenhum recebível encontrado para os filtros selecionados.</p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-800 text-xs uppercase tracking-wider text-slate-500">
                <th className="px-5 py-3 font-medium">ID</th>
                <th className="px-5 py-3 font-medium">Cedente</th>
                <th className="px-5 py-3 font-medium">Tipo</th>
                <th className="px-5 py-3 font-medium">Valor de face</th>
                <th className="px-5 py-3 font-medium">Vencimento</th>
                <th className="px-5 py-3 font-medium">Moeda</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3 text-right font-medium">Ação</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((r) => (
                <tr key={r.id} className="border-b border-slate-800/60 last:border-0 hover:bg-slate-800/30">
                  <td className="px-5 py-3">
                    <span className="font-mono text-xs text-slate-400" title={r.id}>
                      {r.id.slice(0, 8)}…
                    </span>
                  </td>
                  <td className="px-5 py-3">
                    <span className="font-mono text-xs text-slate-400" title={r.cedenteId}>
                      {r.cedenteId.slice(0, 8)}…
                    </span>
                  </td>
                  <td className="px-5 py-3 text-slate-300">{r.receivableTypeName}</td>
                  <td className="px-5 py-3 font-medium text-slate-100">
                    <Money value={r.faceValue} currency={r.currency} />
                  </td>
                  <td className="px-5 py-3 text-slate-300">{formatDate(r.dueDate)}</td>
                  <td className="px-5 py-3 text-slate-400">{r.currency}</td>
                  <td className="px-5 py-3">
                    <Badge status={r.status} />
                  </td>
                  <td className="px-5 py-3 text-right">
                    {r.status === 'PENDING' ? (
                      <Button type="button" variant="secondary" onClick={() => onSettle(r.id)} className="px-2.5 py-1.5 text-xs">
                        <CheckCircle2 className="h-3.5 w-3.5" />
                        Liquidar
                      </Button>
                    ) : (
                      <span className="text-xs text-slate-600">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}
