import { AlertCircle } from 'lucide-react';

interface ErrorAlertProps {
  message: string;
  details?: string;
  onDismiss?: () => void;
}

/** Alerta de erro com correlationId quando disponível. */
export function ErrorAlert({ message, details, onDismiss }: ErrorAlertProps) {
  return (
    <div
      role="alert"
      className="flex items-start gap-3 rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200"
    >
      <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-rose-400" />
      <div className="min-w-0 flex-1">
        <p className="font-medium">{message}</p>
        {details && <p className="mt-0.5 break-all text-xs text-rose-300/80">{details}</p>}
      </div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          className="shrink-0 rounded-md p-1 text-rose-300 transition-colors hover:bg-rose-500/20"
          aria-label="Fechar"
        >
          ✕
        </button>
      )}
    </div>
  );
}
