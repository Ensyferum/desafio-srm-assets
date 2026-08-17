import { useEffect, useState, type FormEvent } from 'react';
import { Download, Filter } from 'lucide-react';
import { Badge } from '../components/Badge';
import { Button, Field, Select, TextInput } from '../components/Field';
import { Card } from '../components/Card';
import { ErrorAlert } from '../components/ErrorAlert';
import { Money } from '../components/Money';
import { Spinner } from '../components/Spinner';
import { Pagination } from '../components/Pagination';
import { api } from '../lib/api';
import { formatDateTime, formatDocument, formatNumber, todayISO } from '../lib/format';
import { useToast } from '../lib/toast';
import { useAsync } from '../lib/useAsync';
import type { PageResponse, TransactionSummary } from '../lib/types';

const PAGE_SIZE = 10;

/** Extrato de liquidações com filtros e paginação server-side (RF05). */
export function ExtratoPage() {
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [currency, setCurrency] = useState('');
  const [cedenteDocument, setCedenteDocument] = useState('');
  const [page, setPage] = useState(0);
  // Filtros efetivamente aplicados (draft é separado para evitar reload por tecla)
  const [applied, setApplied] = useState({ startDate: '', endDate: '', currency: '', cedenteDocument: '' });
  const { push } = useToast();

  const buildUrl = () => {
    const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE), sort: 'settledAt,desc' });
    if (applied.startDate) params.set('startDate', applied.startDate);
    if (applied.endDate) params.set('endDate', applied.endDate);
    if (applied.currency) params.set('currency', applied.currency);
    if (applied.cedenteDocument) params.set('cedenteDocument', applied.cedenteDocument);
    return `/transactions?${params.toString()}`;
  };

  const result = useAsync<PageResponse<TransactionSummary>>(() => api.get(buildUrl()), [page, applied]);

  useEffect(() => {
    setPage(0);
  }, [applied]);

  function handleApply(event: FormEvent) {
    event.preventDefault();
    setApplied({ startDate, endDate, currency, cedenteDocument });
    setPage(0);
  }

  /** Exporta a página atual como CSV (operação/auditoria). */
  function handleExportCsv() {
    if (!result.data || result.data.content.length === 0) {
      push('info', 'Nenhuma linha para exportar com os filtros atuais.');
      return;
    }
    const header = 'data;documento_cedente;valor_face;valor_presente;desconto;moeda;liquidacao;cambio;status';
    const lines = result.data.content.map((tx) =>
      [
        tx.settledAt,
        tx.cedenteDocument,
        tx.faceValue,
        tx.presentValue,
        tx.discountValue,
        tx.currency,
        tx.settlementCurrency,
        tx.exchangeRateApplied ?? '',
        tx.status,
      ].join(';'),
    );
    const blob = new Blob([`${header}\n${lines.join('\n')}`], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `extrato-liquidacoes-${todayISO()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    push('success', 'Arquivo CSV exportado.');
  }

  const data = result.data;

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-xl font-bold tracking-tight text-slate-100">Extrato de liquidações</h1>
        <p className="text-sm text-slate-400">Consulta analítica com paginação server-side · RF05</p>
      </header>

      <Card title="Filtros" subtitle="Período, documento do cedente e moeda de liquidação">
        <form onSubmit={handleApply} className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <Field label="De">
            <TextInput type="date" max={endDate || undefined} value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          </Field>
          <Field label="Até">
            <TextInput type="date" min={startDate || undefined} max={todayISO()} value={endDate} onChange={(e) => setEndDate(e.target.value)} />
          </Field>
          <Field label="CNPJ do cedente" hint="Somente os 14 dígitos">
            <TextInput
              value={cedenteDocument}
              onChange={(e) => setCedenteDocument(e.target.value.replace(/\D/g, ''))}
              placeholder="11222333000181"
              maxLength={14}
              inputMode="numeric"
              className="font-mono text-xs"
            />
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
            {data && data.content.length > 0 && (
              <Button type="button" variant="secondary" onClick={handleExportCsv} className="px-3">
                <Download className="h-4 w-4" />
                <span className="hidden sm:inline">Exportar CSV</span>
              </Button>
            )}
            {data && (
              <Pagination
                page={data.page}
                totalPages={data.totalPages}
                first={data.first}
                last={data.last}
                onChange={setPage}
              />
            )}
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
                  <th className="px-5 py-3 font-medium">Cedente</th>
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
                    <td className="whitespace-nowrap px-5 py-3 font-mono text-xs text-slate-400">
                      {formatDocument(tx.cedenteDocument)}
                    </td>
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
