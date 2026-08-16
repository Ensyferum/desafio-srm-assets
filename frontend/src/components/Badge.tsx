const statusStyles: Record<string, string> = {
  PENDING: 'bg-amber-500/10 text-amber-400 ring-amber-500/30',
  PRICED: 'bg-sky-500/10 text-sky-400 ring-sky-500/30',
  SETTLED: 'bg-emerald-500/10 text-emerald-400 ring-emerald-500/30',
  COMPLETED: 'bg-emerald-500/10 text-emerald-400 ring-emerald-500/30',
  CANCELLED: 'bg-rose-500/10 text-rose-400 ring-rose-500/30',
  FAILED: 'bg-rose-500/10 text-rose-400 ring-rose-500/30',
  REVERSED: 'bg-slate-500/10 text-slate-400 ring-slate-500/30',
};

/** Badge de status com cores semânticas. */
export function Badge({ status }: { status: string }) {
  const style = statusStyles[status] ?? 'bg-slate-500/10 text-slate-300 ring-slate-500/30';
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ${style}`}>
      {status}
    </span>
  );
}
