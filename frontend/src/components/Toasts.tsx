import type { ReactNode } from 'react';
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react';
import { useToast, type ToastItem, type ToastKind } from '../lib/toast';

const kindStyles: Record<ToastKind, { box: string; icon: ReactNode }> = {
  success: {
    box: 'border-emerald-500/30 bg-emerald-950/90',
    icon: <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400" />,
  },
  error: {
    box: 'border-rose-500/30 bg-rose-950/90',
    icon: <AlertCircle className="h-4 w-4 shrink-0 text-rose-400" />,
  },
  info: {
    box: 'border-sky-500/30 bg-sky-950/90',
    icon: <Info className="h-4 w-4 shrink-0 text-sky-400" />,
  },
};

function ToastCard({ toast }: { toast: ToastItem }) {
  const { dismiss } = useToast();
  const style = kindStyles[toast.kind];
  return (
    <div
      role="status"
      className={`pointer-events-auto flex w-80 items-start gap-3 rounded-xl border px-4 py-3 text-sm text-slate-100 shadow-xl shadow-black/40 backdrop-blur ${style.box}`}
    >
      {style.icon}
      <p className="min-w-0 flex-1 break-words">{toast.message}</p>
      <button
        type="button"
        onClick={() => dismiss(toast.id)}
        className="shrink-0 rounded-md p-1 text-slate-400 transition-colors hover:bg-white/10 hover:text-slate-100"
        aria-label="Fechar notificação"
      >
        <X className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}

/** Pilha de notificações fixa no canto superior direito. */
export function Toasts() {
  const { toasts } = useToast();
  if (toasts.length === 0) return null;
  return (
    <div className="pointer-events-none fixed right-4 top-4 z-[100] flex flex-col gap-2">
      {toasts.map((toast) => (
        <ToastCard key={toast.id} toast={toast} />
      ))}
    </div>
  );
}
