import { formatMoney } from '../lib/format';
import type { LucideIcon } from 'lucide-react';

interface StatCardProps {
  label: string;
  value: number;
  /** Moeda para formatação; omita para exibir número inteiro (ex.: contagem). */
  currency?: string;
  icon: LucideIcon;
  hint?: string;
  accent?: 'emerald' | 'sky' | 'amber' | 'violet';
}

const accents = {
  emerald: 'bg-emerald-500/10 text-emerald-400 ring-emerald-500/20',
  sky: 'bg-sky-500/10 text-sky-400 ring-sky-500/20',
  amber: 'bg-amber-500/10 text-amber-400 ring-amber-500/20',
  violet: 'bg-violet-500/10 text-violet-400 ring-violet-500/20',
};

/** Card de KPI com ícone colorido. */
export function StatCard({ label, value, currency, icon: Icon, hint, accent = 'emerald' }: StatCardProps) {
  const display = currency ? formatMoney(value, currency) : new Intl.NumberFormat('pt-BR').format(value);
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5 shadow-lg shadow-black/20 backdrop-blur transition-colors hover:border-slate-700">
      <div className="flex items-center gap-3">
        <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ring-1 ${accents[accent]}`}>
          <Icon className="h-5 w-5" />
        </div>
        <p className="text-xs font-medium uppercase tracking-wider text-slate-400">{label}</p>
      </div>
      <p className="mt-4 text-2xl font-bold tabular-nums text-slate-50">{display}</p>
      {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
    </div>
  );
}
