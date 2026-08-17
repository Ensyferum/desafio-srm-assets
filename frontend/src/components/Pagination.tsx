import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from './Field';

interface PaginationProps {
  /** Página atual (base 0). */
  page: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  onChange: (page: number) => void;
}

/** Controles de paginação server-side reutilizáveis. */
export function Pagination({ page, totalPages, first, last, onChange }: PaginationProps) {
  return (
    <div className="flex items-center gap-2">
      <Button
        type="button"
        variant="secondary"
        disabled={first}
        onClick={() => onChange(Math.max(0, page - 1))}
        className="px-2.5"
        aria-label="Página anterior"
      >
        <ChevronLeft className="h-4 w-4" />
      </Button>
      <span className="text-xs tabular-nums text-slate-400">
        {page + 1}/{Math.max(totalPages, 1)}
      </span>
      <Button
        type="button"
        variant="secondary"
        disabled={last}
        onClick={() => onChange(page + 1)}
        className="px-2.5"
        aria-label="Próxima página"
      >
        <ChevronRight className="h-4 w-4" />
      </Button>
    </div>
  );
}
