/** Tipos espelhando os DTOs do backend (contract-first). */

export type Role = 'ADMIN' | 'MANAGER' | 'OPERATOR';

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  username: string;
  fullName: string;
  role: Role;
}

export interface UserResponse {
  id: string;
  username: string;
  fullName: string;
  role: Role;
  active: boolean;
  createdAt: string;
}

export interface CurrencyResponse {
  id: string;
  code: string;
  name: string;
  symbol: string;
}

export interface ExchangeRateResponse {
  fromCurrency: string;
  toCurrency: string;
  rate: number;
  effectiveDate: string;
}

export interface ExchangeRateRequest {
  fromCurrency: string;
  toCurrency: string;
  rate: number;
  effectiveDate: string;
}

export interface ReceivableTypeResponse {
  id: string;
  name: string;
  spreadMonthly: number;
  description: string;
}

export interface PriceSimulationRequest {
  faceValue: number;
  dueDate: string; // yyyy-MM-dd
  receivableTypeId: string;
  currency: 'BRL' | 'USD';
  settlementCurrency: 'BRL' | 'USD';
  baseRate?: number;
}

export interface PriceSimulationResponse {
  faceValue: number;
  presentValue: number;
  discountValue: number;
  spreadApplied: number;
  termMonths: number;
  baseRate: number;
  /** null quando as moedas são iguais (sem conversão). */
  exchangeRateApplied: number | null;
  presentValueInSettlementCurrency: number;
  currency: string;
  settlementCurrency: string;
  receivableTypeName: string;
}

export type ReceivableStatus = 'PENDING' | 'PRICED' | 'SETTLED' | 'CANCELLED';

export interface ReceivableResponse {
  id: string;
  /** CNPJ do cedente (14 dígitos). */
  cedenteDocument: string;
  receivableTypeId: string;
  receivableTypeName: string;
  faceValue: number;
  dueDate: string;
  currency: string;
  status: ReceivableStatus;
  version: number;
}

export interface CreateReceivableRequest {
  cedenteDocument: string;
  receivableTypeId: string;
  faceValue: number;
  dueDate: string;
  currency: 'BRL' | 'USD';
}

export interface CreateReceivablesBatchRequest {
  receivables: CreateReceivableRequest[];
}

export interface CreateReceivablesBatchResponse {
  created: number;
  receivables: ReceivableResponse[];
}

export interface SettleRequest {
  settlementCurrency: 'BRL' | 'USD';
}

export interface SettleResponse {
  transactionId: string;
  status: string;
  presentValue: number;
  discountValue: number;
  settlementCurrency: string;
  /** null quando as moedas são iguais (sem conversão). */
  exchangeRateApplied: number | null;
  presentValueInSettlementCurrency: number;
  settledAt: string;
}

export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED';

export interface TransactionSummary {
  transactionId: string;
  receivableId: string;
  /** CNPJ do cedente (14 dígitos). */
  cedenteDocument: string;
  faceValue: number;
  presentValue: number;
  discountValue: number;
  currency: string;
  settlementCurrency: string;
  /** null quando as moedas são iguais (sem conversão). */
  exchangeRateApplied: number | null;
  status: TransactionStatus;
  settledAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface AnalyticsSummaryResponse {
  totalTransactions: number;
  totalPresentValue: number;
  totalDiscountValue: number;
  presentValueByCurrency: Record<string, number>;
  startDate: string;
  endDate: string;
}

export interface TimeSeriesPoint {
  /** yyyy-MM-dd */
  date: string;
  currency: string;
  transactions: number;
  presentValue: number;
}

export interface CedenteDistribution {
  /** CNPJ do cedente (14 dígitos). */
  cedenteDocument: string;
  transactions: number;
  presentValue: number;
}

export interface ErrorResponseBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  correlationId: string;
  errorId?: string;
  fieldErrors?: Record<string, string>;
}
