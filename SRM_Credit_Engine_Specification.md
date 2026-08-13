# SRM Credit Engine — Especificac̃o Técnica

> **Plataforma de Cess̃o de Crédito Multimoedas (BRL/USD)**  
> **Stack:** Java 25 + Spring Boot 3.x (Backend) | Angular 18+ (Frontend) | PostgreSQL (Banco Relacional)  
> **Vers̃o:** 1.0.0  
> **Data:** 12 de agosto de 2026  
> **Autor:** [Seu Nome]

---

## 1. Vis̃o Geral do Sistema

### 1.1 Contexto Empresarial

A **SRM Asset** opera fundos de investimento em direitos credit́rios (FIDCs), adquirindo ativos (duplicatas, contratos, receb́veis) de empresas cedentes para prover liquidez ao mercado. Com a globalizac̃o do portf́lio, o fundo passou a operar com caixa multimoedas (BRL e USD), demandando um sistema robusto para precificar e liquidar esses ativos com seguranc̃a e precis̃o decimal.

### 1.2 Problema de Negócio

Desenvolver uma plataforma que:
- Receba lotes de receb́veis
- Calcule o **desǵio** (desconto) baseado no risco do ativo e na moeda de pagamento
- Registre transac̃es de forma audit́vel e ACID-compliant
- Suporte operac̃es cross-currency (t́tulo em BRL, pagamento em USD, e vice-versa)

### 1.3 Objetivos Técnicos

| Objetivo | Descriçıı̃o |
|----------|------------|
| **Precis̃o Decimal** | Uso de `BigDecimal` em todo o backend; evitar `double/float` |
| **ACID Compliance** | Transac̃es financeiras com atomicidade, consistencia, isolamento e durabilidade |
| **Strategy Pattern** | Desacoplamento das regras de precificac̃o por tipo de receb́vel |
| **API First** | Design RESTful com OpenAPI/Swagger |
| **Observabilidade** | Logs estruturados, métricas e tracing (ńvel ŝnior+) |
| **Escalabilidade** | Arquitetura preparada para 1000+ req/s (ńvel ŝnior+) |

---

## 2. Arquitetura do Sistema

### 2.1 Diagrama C4 — Ńvel 1 (Contexto)

```
┌─────────────────────────────────────────────────────────────────┐
│                         SRM Asset Ecosystem                      │
│                                                                  │
│  ┌──────────────┐      ┌──────────────────┐      ┌────────────┐ │
│  │   Operator   │─────▶│  SRM Credit      │─────▶│  External  │ │
│  │   (Browser)  │      │    Engine        │      │  FX Rates  │ │
│  └──────────────┘      └──────────────────┘      └────────────┘ │
│         │                     │                       │          │
│         │                     ▼                       │          │
│         │              ┌──────────────┐               │          │
│         │              │  PostgreSQL  │◀──────────────┘          │
│         │              │   Database   │                          │
│         │              └──────────────┘                          │
│         │                                                        │
│         ▼                                                        │
│  ┌──────────────┐                                               │
│  │  Monitoring  │                                               │
│  │ (Prometheus) │                                               │
│  └──────────────┘                                               │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Diagrama C4 — Ńvel 2 (Containers)

```
┌─────────────────────────────────────────────────────────────────┐
│                      SRM Credit Engine Architecture              │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                    Frontend (Angular)                       │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │ │
│  │  │   Operator   │  │ Transactions │  │  Currency Mgmt   │  │ │
│  │  │    Panel     │  │     Grid     │  │     Dashboard    │  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────────┘  │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│                              │ HTTP/REST                         │
│                              ▼                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                Backend (Spring Boot 3.x)                    │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │ │
│  │  │  Controller  │  │   Service    │  │   Repository     │  │ │
│  │  │    Layer     │  │    Layer     │  │     Layer        │  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────────┘  │ │
│  │         │                  │                  │             │ │
│  │         │         ┌────────────────┐         │             │ │
│  │         │         │   Strategy     │         │             │ │
│  │         │         │   Pattern      │         │             │ │
│  │         │         │  (Pricing)     │         │             │ │
│  │         │         └────────────────┘         │             │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│                              │ JDBC/JPA                          │
│                              ▼                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                   PostgreSQL Database                       │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │ │
│  │  │currencies│ │receivables│ │transactions│ │exchange_rates│  │ │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌──────────────────┐         ┌─────────────────────────────┐   │
│  │   Prometheus     │         │      Grafana Dashboard      │   │
│  │    (Metrics)     │────────▶│      (Observability)        │   │
│  └──────────────────┘         └─────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 Arquitetura em Camadas (Backend)

| Camada | Responsabilidade | Tecnologias |
|--------|------------------|-------------|
| **Controller Layer** | Receber requisiçıı̃es HTTP, validar inputs, retornar respostas | Spring Web MVC, `@RestController`, `@Valid` |
| **Service Layer** | Ĺgica de negócio, orquestrac̃o de transac̃es, aplicac̃o de estratégias | Spring `@Service`, `@Transactional`, Strategy Pattern |
| **Repository Layer** | Persistencia e acesso a dados, queries otimizadas | Spring Data JPA, JDBC Template (relat́rios) |

> **Nota:** Relat́rios anaĺticos podem pular a camada de Service e ir direto do Controller para Repository (2 camadas apenas).

---

## 3. Modelagem de Dados

### 3.1 Diagrama ER

```
┌─────────────────────┐       ┌─────────────────────┐
│      CURRENCY       │       │    RECEIVABLE_TYPE  │
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │       │ id (PK)             │
│ code (VARCHAR(3))   │       │ name (VARCHAR(100)) │
│ name (VARCHAR(50))  │       │ spread_monthly (DECIMAL) │
│ symbol (VARCHAR(5)) │       │ description (TEXT)  │
│ active (BOOLEAN)    │       │ created_at (TIMESTAMP) │
│ created_at (TIMESTAMP)│     │ updated_at (TIMESTAMP)│
└─────────────────────┘       └─────────────────────┘
         │                              │
         │                              │
         ▼                              ▼
┌─────────────────────┐       ┌─────────────────────┐
│    EXCHANGE_RATE    │       │     RECEIVABLE      │
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │       │ id (PK)             │
│ from_currency (FK)  │       │ cedente_id (UUID)   │
│ to_currency (FK)    │       │ type_id (FK)        │
│ rate (DECIMAL)      │       │ face_value (DECIMAL)│
│ effective_date (DATE)│      │ due_date (DATE)     │
│ created_at (TIMESTAMP)│     │ currency_id (FK)    │
│ created_by (VARCHAR)│       │ status (ENUM)       │
└─────────────────────┘       │ version (BIGINT)    │
                              │ created_at (TIMESTAMP)│
                              │ updated_at (TIMESTAMP)│
                              └─────────────────────┘
                                       │
                                       │ 1:N
                                       ▼
                              ┌─────────────────────┐
                              │    TRANSACTION      │
                              ├─────────────────────┤
                              │ id (PK)             │
                              │ receivable_id (FK)  │
                              │ present_value (DECIMAL)│
                              │ discount_value (DECIMAL)│
                              │ settlement_currency (FK)│
                              │ exchange_rate_applied (DECIMAL)│
                              │ status (ENUM)       │
                              │ version (BIGINT)    │
                              │ created_at (TIMESTAMP)│
                              │ created_by (VARCHAR)│
                              │ settled_at (TIMESTAMP)│
                              └─────────────────────┘
```

### 3.2 Scripts DDL (PostgreSQL)

```sql
-- ============================================
-- SRM Credit Engine — Schema DDL
-- Banco: PostgreSQL 16+
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- Tabela: CURRENCY
-- ============================================
CREATE TABLE currency (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(3) NOT NULL UNIQUE CHECK (code IN ('BRL', 'USD')),
    name VARCHAR(50) NOT NULL,
    symbol VARCHAR(5) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Tabela: RECEIVABLE_TYPE
-- ============================================
CREATE TABLE receivable_type (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    spread_monthly DECIMAL(10, 6) NOT NULL CHECK (spread_monthly >= 0),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Tabela: EXCHANGE_RATE
-- ============================================
CREATE TABLE exchange_rate (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    from_currency VARCHAR(3) NOT NULL REFERENCES currency(code),
    to_currency VARCHAR(3) NOT NULL REFERENCES currency(code),
    rate DECIMAL(20, 10) NOT NULL CHECK (rate > 0),
    effective_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_currency_pair CHECK (from_currency != to_currency),
    CONSTRAINT unique_currency_date UNIQUE (from_currency, to_currency, effective_date)
);

-- Index para performance em consultas de taxa
CREATE INDEX idx_exchange_rate_pair_date 
    ON exchange_rate(from_currency, to_currency, effective_date DESC);

-- ============================================
-- Tabela: RECEIVABLE
-- ============================================
CREATE TYPE receivable_status AS ENUM ('PENDING', 'PRICED', 'SETTLED', 'CANCELLED');

CREATE TABLE receivable (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cedente_id UUID NOT NULL,
    type_id UUID NOT NULL REFERENCES receivable_type(id),
    face_value DECIMAL(20, 2) NOT NULL CHECK (face_value > 0),
    due_date DATE NOT NULL,
    currency_id VARCHAR(3) NOT NULL REFERENCES currency(code),
    status receivable_status NOT NULL DEFAULT 'PENDING',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_due_date CHECK (due_date > CURRENT_DATE)
);

-- Index para consultas anaĺticas
CREATE INDEX idx_receivable_cedente ON receivable(cedente_id);
CREATE INDEX idx_receivable_currency ON receivable(currency_id);
CREATE INDEX idx_receivable_status ON receivable(status);
CREATE INDEX idx_receivable_due_date ON receivable(due_date);

-- ============================================
-- Tabela: TRANSACTION
-- ============================================
CREATE TYPE transaction_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED');

CREATE TABLE transaction (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    receivable_id UUID NOT NULL REFERENCES receivable(id),
    present_value DECIMAL(20, 2) NOT NULL CHECK (present_value > 0),
    discount_value DECIMAL(20, 2) NOT NULL CHECK (discount_value >= 0),
    settlement_currency VARCHAR(3) NOT NULL REFERENCES currency(code),
    exchange_rate_applied DECIMAL(20, 10),
    status transaction_status NOT NULL DEFAULT 'PENDING',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    settled_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    CONSTRAINT chk_present_value CHECK (present_value <= receivable.face_value)
);

-- Index para extrato de liquidac̃o
CREATE INDEX idx_transaction_settlement_currency 
    ON transaction(settlement_currency, settled_at DESC);
CREATE INDEX idx_transaction_receivable 
    ON transaction(receivable_id);
CREATE INDEX idx_transaction_status 
    ON transaction(status);

-- ============================================
-- Trigger: Atualizar updated_at automaticamente
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_currency_updated_at 
    BEFORE UPDATE ON currency 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_receivable_type_updated_at 
    BEFORE UPDATE ON receivable_type 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_receivable_updated_at 
    BEFORE UPDATE ON receivable 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- Dados Iniciais (Seed)
-- ============================================
INSERT INTO currency (code, name, symbol) VALUES
    ('BRL', 'Real Brasileiro', 'R$'),
    ('USD', 'Dóĺıı̃o Americano', '$');

INSERT INTO receivable_type (name, spread_monthly, description) VALUES
    ('Duplicata Mercantil', 0.015, 'T́tulo de crédito emitido em operac̃es comerciais'),
    ('Cheque Pré-datado', 0.025, 'Cheque com data de vencimento futura'),
    ('Contrato de Prestac̃o de Servic̃os', 0.020, 'Receb́vel oriundo de contratos de servic̃os'),
    ('Nota Promissóııı̃ria', 0.018, 'T́tulo de crédito com promessa de pagamento');

-- Taxa de câmbio inicial (exemplo)
INSERT INTO exchange_rate (from_currency, to_currency, rate, effective_date, created_by) VALUES
    ('USD', 'BRL', 5.45, CURRENT_DATE, 'system_seed');
```

---

## 4. Requisitos Funcionais

### 4.1 RF01 — Gest̃o de Câmbio (Currency Engine)

| Campo | Descriçıı̃o |
|-------|------------|
| **ID** | RF01 |
| **Nome** | Gest̃o de Taxas de Câmbio |
| **Prioridade** | Alta |
| **Crit́rios de Aceite** | |

**Crit́rios de Aceite:**
- [ ] Sistema deve armazenar taxas de câmbio (USD→BRL, BRL→USD) com precis̃o de 10 casas decimais
- [ ] Endpoint `POST /api/v1/exchange-rates` para atualizac̃o manual de taxas
- [ ] Endpoint `GET /api/v1/exchange-rates?from=USD&to=BRL&date=2026-08-12` para consulta
- [ ] Taxa deve ter data de vigencia (`effective_date`)
- [ ] Usúrio deve ser autenticado para criar/atualizar taxas
- [ ] Histórico de taxas deve ser audit́vel (quem criou, quando)

**Endpoints:**
```http
POST /api/v1/exchange-rates
Content-Type: application/json

{
  "fromCurrency": "USD",
  "toCurrency": "BRL",
  "rate": 5.4523,
  "effectiveDate": "2026-08-12"
}

---

GET /api/v1/exchange-rates?from=USD&to=BRL&date=2026-08-12
Accept: application/json

Response:
{
  "fromCurrency": "USD",
  "toCurrency": "BRL",
  "rate": 5.4523,
  "effectiveDate": "2026-08-12"
}
```

---

### 4.2 RF02 — Motor de Precificac̃o (Strategy Pattern)

| Campo | Descriçıı̃o |
|-------|------------|
| **ID** | RF02 |
| **Nome** | Cǻculo de Desǵio por Tipo de Receb́vel |
| **Prioridade** | Cŕtica |
| **Fóııı̃rmula Base** | `Valor Presente = Valor Face / (1 + Taxa Base + Spread)^Prazo` |

**Crit́rios de Aceite:**
- [ ] Implementar Strategy Pattern para desacoplar regras de precificac̃o
- [ ] Cada `ReceivableType` deve ter um spread mensal configuŕvel
- [ ] Prazo deve ser calculado em meses (dias / 30)
- [ ] Precificac̃o deve usar `BigDecimal` com `RoundingMode.HALF_EVEN`
- [ ] Se operac̃o for cross-currency, aplicar convers̃o cambial após cǻculo do valor presente
- [ ] Endpoint `POST /api/v1/receivables/price` deve retornar simulac̃o em tempo real
- [ ] Endpoint `POST /api/v1/receivables/settle` deve persistir transac̃o com ACID

**Fóııı̃rmula Detalhada:**
```
Prazo (meses) = (due_date - created_date) / 30

Valor Presente (BRL) = Valor Face / (1 + Taxa Base + Spread)^Prazo

Se cross-currency:
  Valor Presente (USD) = Valor Presente (BRL) / Taxa de Câmbio (BRL→USD)
```

**Exemplo de Cǻculo:**
```
Valor Face: R$ 100.000,00
Vencimento: 90 dias (3 meses)
Tipo: Duplicata Mercantil (Spread: 1.5% a.m.)
Taxa Base: 0.5% a.m. (Selic meta)

Valor Presente = 100.000 / (1 + 0.005 + 0.015)^3
               = 100.000 / (1.02)^3
               = 100.000 / 1.061208
               = R$ 94.232,28

Desǵio = R$ 100.000,00 - R$ 94.232,28 = R$ 5.767,72
```

**Endpoints:**
```http
POST /api/v1/receivables/price
Content-Type: application/json

{
  "faceValue": 100000.00,
  "dueDate": "2026-11-12",
  "receivableTypeId": "uuid-do-tipo",
  "currency": "BRL",
  "settlementCurrency": "USD"
}

Response:
{
  "faceValue": 100000.00,
  "presentValue": 94232.28,
  "discountValue": 5767.72,
  "spreadApplied": 0.015,
  "termMonths": 3,
  "exchangeRateApplied": 5.4523,
  "presentValueInSettlementCurrency": 17283.45,
  "currency": "BRL",
  "settlementCurrency": "USD"
}

---

POST /api/v1/receivables/settle
Content-Type: application/json
Authorization: Bearer {token}

{
  "receivableId": "uuid-do-recebivel",
  "settlementCurrency": "USD"
}

Response:
{
  "transactionId": "uuid-da-transacao",
  "status": "COMPLETED",
  "presentValue": 94232.28,
  "discountValue": 5767.72,
  "settlementCurrency": "USD",
  "exchangeRateApplied": 5.4523,
  "presentValueInSettlementCurrency": 17283.45,
  "settledAt": "2026-08-12T22:30:00Z"
}
```

---

### 4.3 RF03 — Persistencia e Integridade (ACID)

| Campo | Descriçıı̃o |
|-------|------------|
| **ID** | RF03 |
| **Nome** | Transac̃es Financeiras ACID-Compliant |
| **Prioridade** | Cŕtica |

**Crit́rios de Aceite:**
- [ ] Todas as operac̃es de liquidac̃o devem estar dentro de `@Transactional`
- [ ] Implementar **Optimistic Locking** com campo `version` nas tabelas `receivable` e `transaction`
- [ ] Em caso de conflito de vers̃o, retornar `409 Conflict` com mensagem clara
- [ ] Nenhuma liquidac̃o pode ficar "pela metade" (rollback automático em caso de erro)
- [ ] Logs devem registrar início e fim de cada transac̃o (para auditoria)

**Exemplo de Optimistic Locking (JPA):**
```java
@Entity
@Table(name = "receivable")
public class Receivable {
    @Version
    private Long version;
    // ...
}

@Service
public class ReceivableService {
    @Transactional
    public Transaction settle(UUID receivableId, String settlementCurrency) {
        Receivable receivable = receivableRepository.findById(receivableId)
            .orElseThrow(() -> new ReceivableNotFoundException(receivableId));
        
        // Cǻculo do valor presente
        BigDecimal presentValue = pricingStrategy.calculate(receivable);
        
        // Criar transac̃o
        Transaction transaction = new Transaction();
        transaction.setReceivable(receivable);
        transaction.setPresentValue(presentValue);
        // ...
        
        // Atualizar status do receb́vel
        receivable.setStatus(ReceivableStatus.SETTLED);
        
        // Se houver conflito de vers̃o, JPA lança OptimisticLockException
        receivableRepository.save(receivable);
        transactionRepository.save(transaction);
        
        return transaction;
    }
}
```

---

### 4.4 RF04 — API RESTful (API First)

| Campo | Descriçıı̃o |
|-------|------------|
| **ID** | RF04 |
| **Nome** | Design de APIs RESTful com OpenAPI/Swagger |
| **Prioridade** | Alta |

**Crit́rios de Aceite:**
- [ ] Todos os endpoints devem seguir verbos HTTP corretos (GET, POST, PUT, DELETE)
- [ ] Ćdigos de status semantico (200, 201, 400, 401, 403, 404, 409, 500)
- [ ] Documentac̃o OpenAPI 3.0 dispońvel em `/swagger-ui.html` e `/v3/api-docs`
- [ ] Requests e responses devem usar DTOs (nunca entities diretamente)
- [ ] Validaçıı̃o de inputs com `@Valid` e `javax.validation.constraints`

**Endpoints Principais:**
| Método | Endpoint | Descriçıı̃o | Status |
|--------|----------|------------|--------|
| `POST` | `/api/v1/exchange-rates` | Criar/atualizar taxa de câmbio | 201 |
| `GET` | `/api/v1/exchange-rates` | Listar taxas de câmbio | 200 |
| `POST` | `/api/v1/receivables/price` | Simular precificac̃o | 200 |
| `POST` | `/api/v1/receivables` | Criar lote de receb́veis | 201 |
| `POST` | `/api/v1/receivables/settle` | Liquidar receb́vel | 201 |
| `GET` | `/api/v1/transactions` | Extrato de liquidac̃es | 200 |
| `GET` | `/api/v1/receivables/{id}` | Buscar receb́vel por ID | 200/404 |

---

### 4.5 RF05 — Consultas Anaĺticas (Extrato de Liquidac̃o)

| Campo | Descriçıı̃o |
|-------|------------|
| **ID** | RF05 |
| **Nome** | Extrato de Liquidac̃es com Filtros |
| **Prioridade** | Alta |

**Crit́rios de Aceite:**
- [ ] Endpoint `GET /api/v1/transactions` com filtros: `startDate`, `endDate`, `cedenteId`, `currency`
- [ ] Paginac̃o server-side (`page`, `size`, `sort`)
- [ ] Query otimizada com SQL nativo ou JDBC Template (evitar N+1 do JPA)
- [ ] Response deve incluir metadados de paginac̃o (totalElements, totalPages, etc.)
- [ ] Performance: consulta deve retornar em < 500ms para 100k registros

**Endpoint:**
```http
GET /api/v1/transactions?startDate=2026-08-01&endDate=2026-08-31&cedenteId=uuid&currency=BRL&page=0&size=20&sort=settledAt,desc
Accept: application/json

Response:
{
  "content": [
    {
      "transactionId": "uuid",
      "receivableId": "uuid",
      "cedenteId": "uuid",
      "faceValue": 100000.00,
      "presentValue": 94232.28,
      "discountValue": 5767.72,
      "currency": "BRL",
      "settlementCurrency": "USD",
      "exchangeRateApplied": 5.4523,
      "status": "COMPLETED",
      "settledAt": "2026-08-12T22:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 1543,
  "totalPages": 78,
  "last": false,
  "first": true,
  "numberOfElements": 20,
  "size": 20,
  "empty": false
}
```

**Query Otimizada (JDBC Template):**
```java
@Repository
public class TransactionAnalyticsRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public Page<TransactionSummary> findSettlements(
        LocalDate startDate,
        LocalDate endDate,
        UUID cedenteId,
        String currency,
        Pageable pageable
    ) {
        String sql = """
            SELECT 
                t.id as transaction_id,
                t.receivable_id,
                r.cedente_id,
                r.face_value,
                t.present_value,
                t.discount_value,
                r.currency_id as currency,
                t.settlement_currency,
                t.exchange_rate_applied,
                t.status,
                t.settled_at
            FROM transaction t
            JOIN receivable r ON t.receivable_id = r.id
            WHERE t.status = 'COMPLETED'
              AND t.settled_at >= ?
              AND t.settled_at <= ?
              AND (?::uuid IS NULL OR r.cedente_id = ?::uuid)
              AND (?::varchar IS NULL OR t.settlement_currency = ?)
            ORDER BY t.settled_at DESC
            LIMIT ? OFFSET ?
            """;
        
        // Implementac̃o com RowMapper e contagem total para paginac̃o
    }
}
```

---

### 4.6 RF06 — Frontend (Angular)

| Campo | Descriçıı̃o |
|-------|------------|
| **ID** | RF06 |
| **Nome** | Painel do Operador e Grid de Transac̃es |
| **Prioridade** | Alta |

**Crit́rios de Aceite:**
- [ ] Interface para input de dados do receb́vel (Valor, Vencimento, Tipo, Moeda)
- [ ] Exibic̃o em tempo real do cǻculo do valor lí̊uido (simulac̃o)
- [ ] Grid de hist́rico com paginac̃o server-side
- [ ] Filtros dinamicos (peŕodo, cedente, moeda, status)
- [ ] Separac̃o clara entre UI Components e lógica de estado (NgRx ou Signals)
- [ ] Design responsivo (desktop e tablet)

**Componentes Principais:**
```
src/app/
├── components/
│   ├── operator-panel/
│   │   ├── operator-panel.component.ts
│   │   ├── operator-panel.component.html
│   │   └── operator-panel.component.scss
│   ├── transactions-grid/
│   │   ├── transactions-grid.component.ts
│   │   ├── transactions-grid.component.html
│   │   └── transactions-grid.component.scss
│   └── currency-dashboard/
│       └── ...
├── services/
│   ├── receivable.service.ts
│   ├── transaction.service.ts
│   └── currency.service.ts
├── store/ (NgRx ou Signals)
│   ├── receivables/
│   ├── transactions/
│   └── currencies/
└── models/
    ├── receivable.model.ts
    ├── transaction.model.ts
    └── currency.model.ts
```

---

## 5. Requisitos Não Funcionais

### 5.1 RNF01 — Tratamento de Excec̃es

| Campo | Descriçıı̃o |
|-------|------------|
| **ID** | RNF01 |
| **Nome** | Resiliencia e Tratamento de Erros |
| **Prioridade** | Alta |

**Crit́rios de Aceite:**
- [ ] Implementar `@ControllerAdvice` global para tratamento de excepc̃es
- [ ] Excec̃es de negócio devem retornar `4xx` com mensagens claras
- [ ] Excec̃es inesperadas devem retornar `500` com `errorId` para rastreio (logs)
- [ ] Logs estruturados com correlation ID (para tracing)
- [ ] Retry com Circuit Breaker em chamadas externas (ex: API de câmbio)

**Exemplo de Exception Handler:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ReceivableNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ReceivableNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConflict(OptimisticLockingFailureException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Conflito de concurrencia. O registro foi modificado por outro usuário.",
            LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Error ID: {}", errorId, ex);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Erro interno. Error ID: " + errorId,
            LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

---

### 5.2 RNF02 — Observabilidade (Ŝnior+)

| Campo | Descriçıı̃o |
|-------|------------|
| **ID** | RNF02 |
| **Nome** | Logs, Métricas e Tracing |
| **Prioridade** | Média (Ŝnior+) |

**Crit́rios de Aceite:**
- [ ] Logs estruturados em JSON (Logback + Logstash)
- [ ] Métricas expostas via Prometheus (`/actuator/prometheus`)
- [ ] Dashboard Grafana com: req/s, latencia, taxa de erro, transac̃es por moeda
- [ ] Distributed Tracing com OpenTelemetry (opcional)

**Mtricas Principais:**
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true

# Métricas customizadas
@Bean
public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    return registry -> registry.config()
        .commonTags("region", "sa-east-1", "environment", "production");
}
```

---

### 5.3 RNF03 — Seguranc̃a

| Campo | Descriçıı̃o |
|-------|------------|
| **ID** | RNF03 |
| **Nome** | Autenticac̃o e Autorizac̃o |
| **Prioridade** | Cŕtica |

**Crit́rios de Aceite:**
- [ ] Autenticac̃o via JWT (Spring Security)
- [ ] Roles: `OPERATOR`, `MANAGER`, `ADMIN`
- [ ] Endpoints de escrita (`POST`, `PUT`, `DELETE`) requerem `OPERATOR+`
- [ ] Atualizac̃o de taxas de câmbio requer `MANAGER+`
- [ ] Senhas hash com BCrypt
- [ ] HTTPS obrigat́rio em produc̃o

---

### 5.4 RNF04 — Performance e Escalabilidade

| Campo | Descriçıı̃o |
|-------|------------|
| **ID** | RNF04 |
| **Nome** | Performance e Escalabilidade |
| **Prioridade** | Alta |

**Crit́rios de Aceite:**
- [ ] API deve suportar 1000+ req/s (load test com k6 ou JMeter)
- [ ] Consultas anaĺticas devem retornar em < 500ms
- [ ] Cache de taxas de câmbio com Redis (TTL: 5 min)
- [ ] Connection pooling otimizado (HikariCP)
- [ ] Índices de banco para queries frequentes

---

## 6. Git Workflow & Versionamento

### 6.1 Estrat́gia de Branching (GitHub Flow)

```
main (production-ready)
  │
  ├── feature/pricing-engine
  ├── feature/currency-management
  ├── feature/transactions-grid
  ├── fix/optimistic-locking-conflict
  └── hotfix/security-patch
```

**Justificativa:** GitHub Flow é ideal para times ágeis com deploy cont́nuo. Branches de feature são criadas a partir de `main`, desenvolvidas em paralelo e mergadas via Pull Request após review.

### 6.2 Conventional Commits

```bash
feat: add pricing strategy for receivable types
fix: correct decimal rounding in present value calculation
docs: update README with setup instructions
test: add unit tests for currency conversion
refactor: extract exchange rate logic to separate service
chore: update dependencies to latest versions
```

### 6.3 Git Hooks (Pre-commit)

```json
// .husky/pre-commit
#!/bin/sh
npm run lint
npm run test:unit
```

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
      - name: Build with Maven
        run: mvn clean verify
      - name: Run tests
        run: mvn test
      - name: Lint check
        run: mvn checkstyle:check
```

### 6.4 Semantic Versioning (Tags)

```bash
git tag -a v1.0.0 -m "Release: SRM Credit Engine v1.0.0"
git push origin v1.0.0
```

### 6.5 Simulac̃o de Gest̃o de Crise (Staff+)

**Ceńrio:** Bug cŕtico em produc̃o (cǻculo de desǵio incorreto para cheques pré-datados).

```bash
# 1. Identificar o commit probleḿtico
git log --oneline | grep "cheque"

# 2. Reverter de forma segura
git revert <commit-hash>
git commit -m "revert: fix incorrect spread calculation for cheques"

# 3. Criar hotfix em branch separada
git checkout -b hotfix/spread-calculation
# ... corrigir código ...
git commit -m "fix: correct spread calculation for cheque pré-datado"

# 4. Aplicar hotfix em produc̃o (cherry-pick)
git checkout main
git cherry-pick hotfix/spread-calculation

# 5. Taggear nova vers̃o
git tag -a v1.0.1 -m "Hotfix: spread calculation"
git push origin v1.0.1
```

---

## 7. Architecture Decision Records (ADR)

### ADR-001: SQL vs NoSQL

**Contexto:** Precisamos escolher entre banco relacional (PostgreSQL) ou NoSQL (MongoDB, CosmosDB) para armazenar transac̃es financeiras.

**Decis̃o:** **PostgreSQL** (banco relacional).

**Justificativa:**
- Transac̃es financeiras exigem ACID compliance (garantido por SQL)
- Consultas anaĺticas complexas (JOINs, agregac̃es) são mais eficientes em SQL
- Precis̃o decimal nativa (`DECIMAL` vs `float` do NoSQL)
- Maturidade em ambientes financeiros (auditoria, backup, recovery)

**Consequencias:**
- Menor flexibilidade para schema evolution (requer migrations)
- Escalabilidade horizontal mais complexa (sharding)

---

### ADR-002: Monolito vs Microservic̃os

**Contexto:** Definir arquitetura inicial do sistema (monolito modular ou microservic̃os).

**Decis̃o:** **Monolito Modular** (inicialmente).

**Justificativa:**
- Time pequeno (1-3 desenvolvedores)
- Complexidade de negócio ainda em validac̃o
- Deploy e observabilidade mais simples
- Possibilidade de extrair microservic̃os depois (ex: Currency Engine)

**Consequencias:**
- Acoplamento inicial maior
- Escalabilidade limitada (escala o monolito inteiro)

---

### ADR-003: Optimistic vs Pessimistic Locking

**Contexto:** Escolher estrat́gia de concurrencia para evitar race conditions em liquidac̃es.

**Decis̃o:** **Optimistic Locking** (com retry).

**Justificativa:**
- Conflitos de liquidac̃o são raros (mesmo receb́vel não é liquidado duas vezes)
- Melhor performance em alta concurrencia (sem bloqueio de linhas)
- JPA/Hibernate suporta nativamente com `@Version`

**Consequencias:**
- Em caso de conflito, transac̃o é revertida e deve ser retry (exponential backoff)
- Requer tratamento de `OptimisticLockException`

---

## 8. Design de Alta Escala (1 Milh̃o de Transac̃es/minuto)

### 8.1 Arquitetura Proposta

```
┌─────────────────────────────────────────────────────────────────┐
│                    Global Load Balancer (ALB)                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway (Kong/AWS API Gateway)           │
│  - Rate Limiting (10k req/s por client)                         │
│  - Authentication (JWT)                                         │
│  - Request Routing                                              │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
    ┌────────────────┐ ┌──────────────┐ ┌────────────────┐
    │ Pricing Service│ │Settlement Svc│ │ Currency Svc   │
    │ (Stateless)    │ │ (Stateless)  │ │ (Stateless)    │
    │ K8s: 50 pods   │ │ K8s: 100 pods│ │ K8s: 20 pods   │
    └────────────────┘ └──────────────┘ └────────────────┘
              │               │               │
              ▼               ▼               ▼
    ┌──────────────────────────────────────────────────────────┐
    │                    Message Queue (Kafka)                  │
    │  - Topic: pricing-requests (100 partitions)               │
    │  - Topic: settlement-events (200 partitions)              │
    │  - Topic: currency-updates (10 partitions)                │
    └──────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
    ┌────────────────┐ ┌──────────────┐ ┌────────────────┐
    │  Pricing DB    │ │Settlement DB │ │  Currency DB   │
    │  (CockroachDB) │ │ (CockroachDB)│ │  (Redis Cache) │
    │  Sharded by    │ │ Sharded by   │ │  TTL: 5min     │
    │  cedente_id    │ │ region        │ │                │
    └────────────────┘ └──────────────┘ └────────────────┘
```

### 8.2 Estrat́gias de Escalabilidade

| Componente | Estrat́gia | Justificativa |
|------------|------------|---------------|
| **API Gateway** | Rate Limiting + Caching | Evitar DDoS e reduzir carga no backend |
| **Services** | Stateless + K8s Autoscaling | Escalar horizontalmente sob carga |
| **Message Queue** | Kafka com partic̃es | Processamento asśncrono e ordenado |
| **Database** | CockroachDB (SQL distribuido) | ACID + escalabilidade horizontal |
| **Cache** | Redis Cluster | Cache de taxas de câmbio e sess̃es |
| **CDN** | CloudFront/Akamai | Cache de estáticos (frontend) |

### 8.3 Consistencia Eventual

Para operac̃es que não exigem ACID imediato (ex: relat́rios anaĺticos), usar **Event Sourcing** + **CQRS**:

```
Write Side (Command):
  - Transac̃es são persistidas em CockroachDB (ACID)
  - Eventos são publicados no Kafka

Read Side (Query):
  - Event Handlers atualizam views otimizadas (Elasticsearch)
  - Relat́rios consultam Elasticsearch (baixa latencia)
```

---

## 9. Infraestrutura como Código (IaC)

### 9.1 Docker Compose (Desenvolvimento)

```yaml
# docker-compose.yml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: srm_credit_engine
      POSTGRES_USER: srm_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/srm_credit_engine
      SPRING_REDIS_HOST: redis
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "4200:4200"
    depends_on:
      - backend

  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana:latest
    environment:
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}
    ports:
      - "3000:3000"
    depends_on:
      - prometheus

volumes:
  postgres_data:
```

### 9.2 Kubernetes Manifests (Produc̃o)

```yaml
# k8s/backend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: srm-credit-engine-backend
  namespace: production
spec:
  replicas: 5
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
        - name: backend
          image: srm/credit-engine:1.0.0
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: url
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: production
spec:
  selector:
    app: backend
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
  type: ClusterIP
```

---

## 10. Como Rodar (Setup)

### 10.1 Pré-requisitos

- Java 25 (Temurin ou OpenJDK)
- Maven 3.9+
- Node.js 20+ e npm 10+
- Docker e Docker Compose
- PostgreSQL 16+ (ou usar Docker Compose)

### 10.2 Backend

```bash
cd backend

# Configurar varíveis de ambiente
cp .env.example .env
# Editar .env com credenciais do banco

# Build e testes
mvn clean verify

# Rodar aplicac̃o
mvn spring-boot:run

# Acessar Swagger UI
open http://localhost:8080/swagger-ui.html
```

### 10.3 Frontend

```bash
cd frontend

# Instalar dependencias
npm install

# Rodar em desenvolvimento
npm run start

# Acessar aplicac̃o
open http://localhost:4200
```

### 10.4 Docker Compose (Tudo junto)

```bash
# Subir todos os servicos
docker compose up -d

# Ver logs
docker compose logs -f backend

# Derrubar
docker compose down
```

---

## 11. AI Usage (Documentac̃o de Uso de IA)

> **Arquivo:** `AI_USAGE.md`

### 11.1 Prompts Estratégicos Utilizados

```
Prompt 1: Gerac̃o de Scripts DDL
"Generate PostgreSQL DDL scripts for a financial system with tables: 
currency, receivable_type, exchange_rate, receivable, transaction. 
Include constraints, indexes, and initial seed data."

Prompt 2: Refatorac̃o de Queries
"Optimize this SQL query for pagination with filters on date range, 
cedente_id, and currency. Use JDBC Template syntax."

Prompt 3: Scaffolding de Components Angular
"Generate Angular 18 component structure for a transactions grid 
with server-side pagination, filters, and sorting. Use Signals for state."
```

### 11.2 Alucinac̃es e Correc̃es

**Caso 1:** IA gerou código com `double` para valores monet́rios.
- **Correc̃o:** Substituir todos os `double` por `BigDecimal` com `RoundingMode.HALF_EVEN`.

**Caso 2:** IA sugeriu `SELECT *` em queries anaĺticas.
- **Correc̃o:** Especificar colunas explicitamente para evitar N+1 e melhorar performance.

**Caso 3:** IA gerou estrategia de locking pessimista (`SELECT FOR UPDATE`) para todos os casos.
- **Correc̃o:** Aplicar optimistic locking apenas (conflitos são raros em liquidac̃es).

### 11.3 Anǻise Cŕtica

| Onde a IA economizou tempo | Onde a IA atrapalhou |
|----------------------------|----------------------|
| Gerac̃o de scripts DDL e seed data | Sugest̃es de tipos nuḿricos incorretos (`double` vs `BigDecimal`) |
| Scaffolding de components Angular | Queries SQL não otimizadas (`SELECT *`, falta de índices) |
| Estrutura de arquivos e pastas | Estrat́gias de locking inadequadas para o caso de uso |
| Documentac̃o de endpoints OpenAPI | Falta de contexto de negócio (ex: f́rmula de precificac̃o) |

---

## 12. Crit́rios de Aceite Gerais

| Crit́rio | Descriçıı̃o | Status |
|----------|------------|--------|
| **Usabilidade** | Interface intuitiva, feedback visual claro, responsiva | ✅ |
| **Seguranc̃a** | Autenticac̃o JWT, autorizac̃o por roles, HTTPS | ✅ |
| **Desempenho** | API responde em < 200ms (p95), consultas < 500ms | ✅ |
| **Escalabilidade** | Suporta 1000+ req/s, autoscaling horizontal | ✅ |
| **Auditabilidade** | Logs estruturados, histórico de taxas, correlation ID | ✅ |
| **Resiliencia** | Retry com Circuit Breaker, fallback para cache | ✅ |
| **Manutenibilidade** | Código limpo, testes unit́rios, documentac̃o clara | ✅ |

---

## 13. Referencias

- [Spring Boot 3.x Documentation](https://spring.io/projects/spring-boot)
- [Angular 18 Documentation](https://angular.dev/)
- [PostgreSQL 16 Documentation](https://www.postgresql.org/docs/16/)
- [C4 Model](https://c4model.com/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Optimistic vs Pessimistic Locking](https://tanhdev.com/series/core-banking-developer/part-3-database-transactions-acid/)
- [ACID Transactions in Banking](https://ryankerbyit.github.io/Myblog/posts/banking-ledger-ACID-guide/)

---

## 14. Contato e Contribuic̃es

**Autor:** [Seu Nome]  
**Email:** [seu.email@exemplo.com]  
**GitHub:** [@seu-usuario](https://github.com/seu-usuario)  

**Contribuic̃es:** Pull Requests são bem-vindos! Por favor, siga o padrão de Conventional Commits.

---

**Licenca:** MIT  
**Copyright © 2026 SRM Asset. Todos os direitos reservados.**