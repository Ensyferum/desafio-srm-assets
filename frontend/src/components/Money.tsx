import { formatMoney } from '../lib/format';

interface MoneyProps {
  value: number | null | undefined;
  currency?: string;
  className?: string;
}

/** Exibe valor monetário com tabular-nums (alinhado em tabelas). */
export function Money({ value, currency = 'BRL', className = '' }: MoneyProps) {
  return <span className={`tabular-nums ${className}`}>{formatMoney(value, currency)}</span>;
}
