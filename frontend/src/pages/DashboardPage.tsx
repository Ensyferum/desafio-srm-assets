import { useEffect, useMemo, useState } from 'react';
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import {
  ArrowRightLeft,
  AreaChart as AreaChartIcon,
  BadgeDollarSign,
  BarChart3,
  Landmark,
  LineChart as LineChartIcon,
  PieChart as PieChartIcon,
  ReceiptText,
  RefreshCw,
} from 'lucide-react';
import { Badge } from '../components/Badge';
import { Card } from '../components/Card';
import { ErrorAlert } from '../components/ErrorAlert';
import { Money } from '../components/Money';
import { Spinner } from '../components/Spinner';
import { StatCard } from '../components/StatCard';
import { api } from '../lib/api';
import { formatDate, formatDocument, formatMoney, formatMoneyCompact } from '../lib/format';
import { useAsync } from '../lib/useAsync';
import type {
  AnalyticsSummaryResponse,
  CedenteDistribution,
  PageResponse,
  TimeSeriesPoint,
  TransactionSummary,
} from '../lib/types';

/** Auto-refresh: atualiza o painel a cada N segundos (observação operacional). */
const AUTO_REFRESH_MS = 30_000;

const chartColors = ['#10b981', '#38bdf8', '#a78bfa', '#f59e0b'];

const chartKinds = [
  { id: 'bar', label: 'Barras', Icon: BarChart3 },
  { id: 'line', label: 'Linhas', Icon: LineChartIcon },
  { id: 'donut', label: 'Donut', Icon: PieChartIcon },
] as const;

type ChartKind = (typeof chartKinds)[number]['id'];

const timeKinds = [
  { id: 'area', label: 'Área', Icon: AreaChartIcon },
  { id: 'line', label: 'Linhas', Icon: LineChartIcon },
] as const;

type TimeKind = (typeof timeKinds)[number]['id'];

const tooltipStyle = {
  background: '#0f172a',
  border: '1px solid #334155',
  borderRadius: 12,
  color: '#e2e8f0',
  fontSize: 12,
};

/** Linha da série temporal: data + uma coluna numérica por moeda (para recharts). */
interface SeriesRow {
  date: string;
  [currency: string]: string | number;
}

/** Dashboard com KPIs, séries temporais, concentração por cedente e últimos lançamentos. */
export function DashboardPage() {
  const [chartKind, setChartKind] = useState<ChartKind>('bar');
  const [timeKind, setTimeKind] = useState<TimeKind>('area');
  const summary = useAsync<AnalyticsSummaryResponse>(() => api.get('/analytics/summary'));
  const timeSeries = useAsync<TimeSeriesPoint[]>(() => api.get('/analytics/timeseries'));
  const byCedente = useAsync<CedenteDistribution[]>(() => api.get('/analytics/by-cedente'));
  const recent = useAsync<PageResponse<TransactionSummary>>(() =>
    api.get('/transactions?page=0&size=6&sort=settledAt,desc'),
  );

  const chartData = useMemo(() => {
    const entries = Object.entries(summary.data?.presentValueByCurrency ?? {});
    if (entries.length === 0) return [];
    return entries.map(([currency, value]) => ({ currency, value }));
  }, [summary.data]);

  /** Série temporal: agrega por data, uma coluna por moeda (para linhas/áreas). */
  const seriesByDate = useMemo(() => {
    const points = timeSeries.data ?? [];
    if (points.length === 0) return [];
    const rows = new Map<string, SeriesRow>();
    for (const point of points) {
      const row = rows.get(point.date) ?? { date: point.date };
      const prev = row[point.currency];
      row[point.currency] = (typeof prev === 'number' ? prev : 0) + point.presentValue;
      rows.set(point.date, row);
    }
    return [...rows.values()].sort((a, b) => a.date.localeCompare(b.date));
  }, [timeSeries.data]);

  const seriesCurrencies = useMemo(
    () => [...new Set((timeSeries.data ?? []).map((p) => p.currency))],
    [timeSeries.data],
  );

  /** Top cedentes: ordena por valor presente e limita a 8 para leitura. */
  const cedenteData = useMemo(() => (byCedente.data ?? []).slice(0, 8), [byCedente.data]);
  const maxCedenteValue = cedenteData[0]?.presentValue ?? 1;

  const [autoRefresh, setAutoRefresh] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  function reloadAll() {
    void summary.reload();
    void timeSeries.reload();
    void byCedente.reload();
    void recent.reload();
    setLastUpdated(new Date());
  }

  // Auto-refresh operacional (30s) — desligável para conferência manual
  useEffect(() => {
    if (!autoRefresh) return undefined;
    const timer = window.setInterval(reloadAll, AUTO_REFRESH_MS);
    return () => window.clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoRefresh]);

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-slate-100">Dashboard</h1>
          <p className="text-sm text-slate-400">Visão geral das liquidações do período</p>
        </div>
        <div className="flex items-center gap-2">
          {lastUpdated && (
            <span className="hidden text-xs text-slate-500 sm:inline">
              Atualizado às {lastUpdated.toLocaleTimeString('pt-BR')}
            </span>
          )}
          <button
            type="button"
            onClick={() => setAutoRefresh((v) => !v)}
            title={autoRefresh ? 'Auto-refresh ativado (30s)' : 'Auto-refresh desativado'}
            className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm transition-colors ${
              autoRefresh
                ? 'border-emerald-500/40 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/20'
                : 'border-slate-700 text-slate-400 hover:border-slate-500 hover:text-slate-200'
            }`}
          >
            <RefreshCw className={`h-4 w-4 ${autoRefresh ? 'animate-spin [animation-duration:8s]' : ''}`} />
            {autoRefresh ? 'Auto' : 'Manual'}
          </button>
          <button
            type="button"
            onClick={reloadAll}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 transition-colors hover:border-slate-500 hover:text-slate-100"
          >
            <RefreshCw className="h-4 w-4" />
            Atualizar
          </button>
        </div>
      </header>

      {summary.error && (
        <ErrorAlert message={summary.error} details={summary.correlationId && `correlationId: ${summary.correlationId}`} />
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Transações"
          value={summary.data?.totalTransactions ?? 0}
          currency="BRL"
          icon={ReceiptText}
          hint="Liquidações do período"
          accent="sky"
        />
        <StatCard
          label="Valor presente (BRL)"
          value={summary.data?.totalPresentValue ?? 0}
          icon={Landmark}
          hint="Soma do valor presente"
          accent="emerald"
        />
        <StatCard
          label="Desconto total (BRL)"
          value={summary.data?.totalDiscountValue ?? 0}
          icon={BadgeDollarSign}
          hint="Deságio total aplicado"
          accent="amber"
        />
        <StatCard
          label="Moedas"
          value={Object.keys(summary.data?.presentValueByCurrency ?? {}).length}
          icon={ArrowRightLeft}
          hint={Object.entries(summary.data?.presentValueByCurrency ?? {})
            .map(([c, v]) => `${c}: ${formatMoneyCompact(v)}`)
            .join(' · ') || 'sem dados'}
          accent="violet"
        />
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-5">
        <Card
          title="Evolução diária do valor presente"
          subtitle="Série temporal por moeda no período"
          className="xl:col-span-3"
          actions={
            <div
              className="inline-flex rounded-lg border border-slate-800 bg-slate-800/40 p-0.5"
              role="group"
              aria-label="Tipo de gráfico temporal"
            >
              {timeKinds.map(({ id, label, Icon }) => (
                <button
                  key={id}
                  type="button"
                  onClick={() => setTimeKind(id)}
                  title={label}
                  aria-label={`Série temporal de ${label}`}
                  aria-pressed={timeKind === id}
                  className={`rounded-md p-1.5 transition-colors ${
                    timeKind === id ? 'bg-brand-600 text-white' : 'text-slate-500 hover:text-slate-200'
                  }`}
                >
                  <Icon className="h-4 w-4" />
                </button>
              ))}
            </div>
          }
        >
          {timeSeries.loading ? (
            <Spinner label="Carregando…" />
          ) : seriesByDate.length === 0 ? (
            <p className="py-10 text-center text-sm text-slate-500">Sem liquidações no período.</p>
          ) : (
            <>
              <div className="relative h-64">
                <ResponsiveContainer key={timeKind} width="100%" height="100%">
                  {timeKind === 'area' ? (
                    <AreaChart data={seriesByDate} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                      <defs>
                        {seriesCurrencies.map((currency, index) => (
                          <linearGradient key={currency} id={`grad-${currency}`} x1="0" y1="0" x2="0" y2="1">
                            <stop offset="0%" stopColor={chartColors[index % chartColors.length]} stopOpacity={0.35} />
                            <stop offset="100%" stopColor={chartColors[index % chartColors.length]} stopOpacity={0} />
                          </linearGradient>
                        ))}
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                      <XAxis
                        dataKey="date"
                        stroke="#64748b"
                        fontSize={12}
                        tickLine={false}
                        axisLine={false}
                        tickFormatter={(value) => formatDate(String(value))}
                        minTickGap={28}
                      />
                      <YAxis
                        stroke="#64748b"
                        fontSize={11}
                        tickLine={false}
                        axisLine={false}
                        tickFormatter={(value) => formatMoneyCompact(Number(value))}
                      />
                      <Tooltip
                        cursor={{ stroke: '#475569', strokeDasharray: '4 4' }}
                        contentStyle={tooltipStyle}
                        labelFormatter={(label) => formatDate(String(label))}
                        formatter={(value, name) => [formatMoney(Number(value ?? 0)), String(name)]}
                      />
                      {seriesCurrencies.map((currency, index) => (
                        <Area
                          key={currency}
                          type="monotone"
                          dataKey={currency}
                          name={currency}
                          stroke={chartColors[index % chartColors.length]}
                          strokeWidth={2.5}
                          fill={`url(#grad-${currency})`}
                          dot={false}
                          activeDot={{ r: 4 }}
                        />
                      ))}
                    </AreaChart>
                  ) : (
                    <LineChart data={seriesByDate} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                      <XAxis
                        dataKey="date"
                        stroke="#64748b"
                        fontSize={12}
                        tickLine={false}
                        axisLine={false}
                        tickFormatter={(value) => formatDate(String(value))}
                        minTickGap={28}
                      />
                      <YAxis
                        stroke="#64748b"
                        fontSize={11}
                        tickLine={false}
                        axisLine={false}
                        tickFormatter={(value) => formatMoneyCompact(Number(value))}
                      />
                      <Tooltip
                        contentStyle={tooltipStyle}
                        labelFormatter={(label) => formatDate(String(label))}
                        formatter={(value, name) => [formatMoney(Number(value ?? 0)), String(name)]}
                      />
                      {seriesCurrencies.map((currency, index) => (
                        <Line
                          key={currency}
                          type="monotone"
                          dataKey={currency}
                          name={currency}
                          stroke={chartColors[index % chartColors.length]}
                          strokeWidth={2.5}
                          dot={{ r: 3, fill: chartColors[index % chartColors.length], strokeWidth: 0 }}
                          activeDot={{ r: 5 }}
                        />
                      ))}
                    </LineChart>
                  )}
                </ResponsiveContainer>
              </div>
              <div className="mt-3 flex flex-wrap justify-center gap-x-4 gap-y-1.5 text-xs text-slate-400">
                {seriesCurrencies.map((currency, index) => (
                  <span key={currency} className="inline-flex items-center gap-1.5">
                    <span className="h-2 w-2 rounded-full" style={{ background: chartColors[index % chartColors.length] }} />
                    {currency}
                  </span>
                ))}
              </div>
            </>
          )}
        </Card>

        <Card
          title="Top cedentes"
          subtitle="Concentração do volume liquidado por CNPJ"
          className="xl:col-span-2"
          padding={false}
        >
          {byCedente.loading ? (
            <div className="p-5">
              <Spinner label="Carregando…" />
            </div>
          ) : byCedente.error ? (
            <div className="p-5">
              <ErrorAlert message={byCedente.error} />
            </div>
          ) : cedenteData.length === 0 ? (
            <p className="py-10 text-center text-sm text-slate-500">Sem liquidações no período.</p>
          ) : (
            <>
              <div className="divide-y divide-slate-800/60">
                {cedenteData.map((cedente, index) => {
                  const width = Math.max(4, (cedente.presentValue / maxCedenteValue) * 100);
                  return (
                    <div key={cedente.cedenteDocument} className="flex items-center gap-3 px-5 py-3">
                      <span className="w-5 shrink-0 text-right text-xs font-semibold tabular-nums text-slate-500">
                        {index + 1}º
                      </span>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-baseline justify-between gap-2">
                          <span
                            className="truncate font-mono text-sm text-slate-200"
                            title={formatDocument(cedente.cedenteDocument)}
                          >
                            {formatDocument(cedente.cedenteDocument)}
                          </span>
                          <span className="shrink-0 text-sm font-semibold tabular-nums text-slate-100">
                            {formatMoneyCompact(cedente.presentValue)}
                          </span>
                        </div>
                        <div className="mt-1.5 flex items-center gap-2">
                          <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-800">
                            <div
                              className="h-full rounded-full transition-all duration-500"
                              style={{ width: `${width}%`, background: chartColors[index % chartColors.length] }}
                            />
                          </div>
                          <span className="shrink-0 text-[11px] tabular-nums text-slate-500">
                            {cedente.transactions} {cedente.transactions === 1 ? 'tx' : 'txs'}
                          </span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
              {(byCedente.data?.length ?? 0) > cedenteData.length && (
                <p className="border-t border-slate-800 px-5 py-3 text-center text-xs text-slate-500">
                  +{(byCedente.data?.length ?? 0) - cedenteData.length} cedentes — veja o extrato para a lista completa
                </p>
              )}
            </>
          )}
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-5">
        <Card
          title="Valor presente por moeda"
          subtitle="Distribuição das liquidações no período"
          className="xl:col-span-2"
          actions={
            <div
              className="inline-flex rounded-lg border border-slate-800 bg-slate-800/40 p-0.5"
              role="group"
              aria-label="Tipo de gráfico"
            >
              {chartKinds.map(({ id, label, Icon }) => (
                <button
                  key={id}
                  type="button"
                  onClick={() => setChartKind(id)}
                  title={label}
                  aria-label={`Gráfico de ${label}`}
                  aria-pressed={chartKind === id}
                  className={`rounded-md p-1.5 transition-colors ${
                    chartKind === id ? 'bg-brand-600 text-white' : 'text-slate-500 hover:text-slate-200'
                  }`}
                >
                  <Icon className="h-4 w-4" />
                </button>
              ))}
            </div>
          }
        >
          {summary.loading ? (
            <Spinner label="Carregando…" />
          ) : chartData.length === 0 ? (
            <p className="py-10 text-center text-sm text-slate-500">Sem liquidações no período.</p>
          ) : (
            <>
              <div className="relative h-64">
                <ResponsiveContainer key={chartKind} width="100%" height="100%">
                  {chartKind === 'bar' && (
                    <BarChart data={chartData} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                      <XAxis dataKey="currency" stroke="#64748b" fontSize={12} tickLine={false} axisLine={false} />
                      <YAxis stroke="#64748b" fontSize={11} tickLine={false} axisLine={false} tickFormatter={(v) => formatMoneyCompact(v)} />
                      <Tooltip
                        cursor={{ fill: 'rgba(148,163,184,0.08)' }}
                        contentStyle={tooltipStyle}
                        formatter={(value) => [formatMoney(Number(value ?? 0)), 'Valor presente']}
                      />
                      <Bar dataKey="value" radius={[8, 8, 0, 0]}>
                        {chartData.map((_, index) => (
                          <Cell key={index} fill={chartColors[index % chartColors.length]} />
                        ))}
                      </Bar>
                    </BarChart>
                  )}
                  {chartKind === 'line' && (
                    <LineChart data={chartData} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                      <XAxis dataKey="currency" stroke="#64748b" fontSize={12} tickLine={false} axisLine={false} />
                      <YAxis stroke="#64748b" fontSize={11} tickLine={false} axisLine={false} tickFormatter={(v) => formatMoneyCompact(v)} />
                      <Tooltip
                        contentStyle={tooltipStyle}
                        formatter={(value) => [formatMoney(Number(value ?? 0)), 'Valor presente']}
                      />
                      <Line
                        type="monotone"
                        dataKey="value"
                        stroke="#10b981"
                        strokeWidth={2.5}
                        dot={{ r: 4, fill: '#10b981', strokeWidth: 0 }}
                        activeDot={{ r: 6 }}
                      />
                    </LineChart>
                  )}
                  {chartKind === 'donut' && (
                    <PieChart>
                      <Tooltip
                        contentStyle={tooltipStyle}
                        formatter={(value) => [formatMoney(Number(value ?? 0)), 'Valor presente']}
                      />
                      <Pie
                        data={chartData}
                        dataKey="value"
                        nameKey="currency"
                        innerRadius={62}
                        outerRadius={92}
                        paddingAngle={3}
                        stroke="#0f172a"
                        strokeWidth={2}
                      >
                        {chartData.map((_, index) => (
                          <Cell key={index} fill={chartColors[index % chartColors.length]} />
                        ))}
                      </Pie>
                    </PieChart>
                  )}
                </ResponsiveContainer>
                {chartKind === 'donut' && (
                  <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
                    <span className="text-[10px] uppercase tracking-wider text-slate-500">Total</span>
                    <span className="text-lg font-bold text-slate-100">
                      {formatMoneyCompact(chartData.reduce((sum, d) => sum + d.value, 0))}
                    </span>
                  </div>
                )}
              </div>
              <div className="mt-3 flex flex-wrap justify-center gap-x-4 gap-y-1.5 text-xs text-slate-400">
                {chartData.map((d, index) => (
                  <span key={d.currency} className="inline-flex items-center gap-1.5">
                    <span className="h-2 w-2 rounded-full" style={{ background: chartColors[index % chartColors.length] }} />
                    {d.currency}: {formatMoneyCompact(d.value, d.currency)}
                  </span>
                ))}
              </div>
            </>
          )}
        </Card>

        <Card
          title="Últimas liquidações"
          subtitle="Registradas no extrato (CQRS)"
          className="xl:col-span-3"
          padding={false}
        >
          {recent.loading ? (
            <Spinner label="Carregando…" />
          ) : recent.error ? (
            <div className="p-5">
              <ErrorAlert message={recent.error} />
            </div>
          ) : (recent.data?.content.length ?? 0) === 0 ? (
            <p className="py-10 text-center text-sm text-slate-500">Nenhuma liquidação ainda.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-800 text-xs uppercase tracking-wider text-slate-500">
                    <th className="px-5 py-3 font-medium">Data</th>
                    <th className="px-5 py-3 font-medium">Valor face</th>
                    <th className="px-5 py-3 font-medium">Valor presente</th>
                    <th className="px-5 py-3 font-medium">Moeda</th>
                    <th className="px-5 py-3 font-medium">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {recent.data?.content.map((tx) => (
                    <tr key={tx.transactionId} className="border-b border-slate-800/60 last:border-0 hover:bg-slate-800/30">
                      <td className="px-5 py-3 text-slate-300">{formatDate(tx.settledAt)}</td>
                      <td className="px-5 py-3 text-slate-300">
                        <Money value={tx.faceValue} currency={tx.currency} />
                      </td>
                      <td className="px-5 py-3 font-medium text-slate-100">
                        <Money value={tx.presentValue} currency={tx.currency} />
                      </td>
                      <td className="px-5 py-3 text-slate-400">
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
    </div>
  );
}
