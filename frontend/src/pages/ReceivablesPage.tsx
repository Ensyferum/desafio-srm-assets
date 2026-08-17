import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { CheckCircle2, ListChecks, ListOrdered } from 'lucide-react';
import { BatchTab } from './receivables/BatchTab';
import { SettleTab } from './receivables/SettleTab';
import { ReceivablesListTab } from './receivables/ReceivablesListTab';

type Tab = 'lista' | 'registrar' | 'liquidar';

const TABS: { id: Tab; label: string; Icon: typeof ListChecks }[] = [
  { id: 'lista', label: 'Lista de recebíveis', Icon: ListChecks },
  { id: 'registrar', label: 'Registrar lote', Icon: ListOrdered },
  { id: 'liquidar', label: 'Liquidar', Icon: CheckCircle2 },
];

const TAB_FROM_PARAM: Record<string, Tab | undefined> = {
  lista: 'lista',
  registrar: 'registrar',
  liquidar: 'liquidar',
};

/** Recebíveis: lista paginada, registro em lote (RF02) e liquidação (RF03/RF04). */
export function ReceivablesPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const paramTab = TAB_FROM_PARAM[searchParams.get('tab') ?? ''];
  const [localTab, setLocalTab] = useState<Tab>('lista');
  // A URL é a fonte da verdade quando ?tab= existe; senão usa o estado local.
  const tab: Tab = paramTab ?? localTab;
  const [pendingId, setPendingId] = useState('');

  function handleTabChange(next: Tab) {
    setLocalTab(next);
    setSearchParams({ tab: next }, { replace: true });
  }

  function handleSettleFromList(id: string) {
    setPendingId(id);
    handleTabChange('liquidar');
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-xl font-bold tracking-tight text-slate-100">Recebíveis</h1>
        <p className="text-sm text-slate-400">Lista, registro em lote (RF02) e liquidação (RF03/RF04)</p>
      </header>

      <nav className="inline-flex rounded-xl border border-slate-800 bg-slate-900/60 p-1" aria-label="Seções de recebíveis">
        {TABS.map(({ id, label, Icon }) => (
          <button
            key={id}
            type="button"
            onClick={() => handleTabChange(id)}
            className={`inline-flex items-center gap-2 rounded-lg px-3.5 py-2 text-sm font-medium transition-colors ${
              tab === id
                ? 'bg-brand-600 text-white shadow-sm shadow-brand-900/40'
                : 'text-slate-400 hover:bg-slate-800/70 hover:text-slate-200'
            }`}
            aria-current={tab === id ? 'page' : undefined}
          >
            <Icon className="h-4 w-4" />
            {label}
          </button>
        ))}
      </nav>

      {tab === 'lista' && <ReceivablesListTab onSettle={handleSettleFromList} />}
      {tab === 'registrar' && <BatchTab />}
      {tab === 'liquidar' && <SettleTab pendingId={pendingId} onConsumed={() => setPendingId('')} />}
    </div>
  );
}
