import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { useAuth } from './lib/useAuth';
import { DashboardPage } from './pages/DashboardPage';
import { ExchangeRatesPage } from './pages/ExchangeRatesPage';
import { ExtratoPage } from './pages/ExtratoPage';
import { LoginPage } from './pages/LoginPage';
import { ReceivablesPage } from './pages/ReceivablesPage';
import { SimulationPage } from './pages/SimulationPage';

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { token } = useAuth();
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route path="/" element={<DashboardPage />} />
        <Route path="/simulacao" element={<SimulationPage />} />
        <Route path="/recebiveis" element={<ReceivablesPage />} />
        <Route path="/taxas" element={<ExchangeRatesPage />} />
        <Route path="/extrato" element={<ExtratoPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
