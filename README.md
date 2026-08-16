# SRM Credit Engine

> Plataforma de **cessão de crédito multimoedas (BRL/USD)** — recebe lotes de recebíveis, precifica com deságio por risco, converte câmbio e liquida de forma auditável, com extrato analítico em tempo real.

Desafio técnico **SRM Asset** (FIDC) — [especificação completa](README_case_dev_srm.md) e [spec técnica detalhada](SRM_Credit_Engine_Specification.md).

---

## 📌 Sumário

- [Visão geral](#-visão-geral)
- [Stack](#-stack)
- [Arquitetura](#-arquitetura)
- [Estrutura do repositório](#-estrutura-do-repositório)
- [Como rodar (Docker Compose)](#-como-rodar-docker-compose)
- [Credenciais de demonstração](#-credenciais-de-demonstração)
- [Observabilidade](#-observabilidade)
- [API — referência e exemplos](#-api--referência-e-exemplos)
- [Modelo de dados (ER)](#-modelo-de-dados-er)
- [Testes, qualidade e CI/CD](#-testes-qualidade-e-cicd)
- [Decisões de arquitetura (ADRs)](#-decisões-de-arquitetura-adrs)
- [Estratégia de Git](#-estratégia-de-git)
- [Design para alta escala (1M tx/min)](#-design-para-alta-escala-1m-txmin)
- [Roadmap](#-roadmap)

---

## 🏢 Visão geral

O fundo **SRM Asset** opera FIDCs e compra ativos (duplicatas, cheques, recebíveis) de empresas cedentes, com caixa multimoedas (BRL e USD). O **SRM Credit Engine** automatiza o ciclo:

1. **Câmbio (RF01)** — gestão de taxas de câmbio (mock de integração + atualização manual).
2. **Precificação (RF02)** — valor presente = `Valor Face / (1 + Taxa Base + Spread)^Prazo`, com **Strategy Pattern** por tipo de recebível e conversão cambial em operações cross-currency.
3. **Liquidação (RF03/RF04)** — transação **ACID** com *optimistic locking* (nenhuma liquidação fica pela metade), auditável, publicando evento no Kafka.
4. **Analytics (RF05)** — extrato de liquidações filtrado por período/cedente/moeda via **CQRS** (projeção de leitura alimentada por eventos) e resumo para dashboard.

**Fluxo ponta a ponta verificado por smoke test: `10/10` checks.** [Ver script](scripts/e2e-smoke.sh).

---

## 🧰 Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | **Java 25** (Temurin) |
| Framework | **Spring Boot 4.1** (Spring MVC, WebFlux no gateway) |
| API Gateway | **Spring Cloud Gateway 5.0** (SCG) |
| Banco de dados | **PostgreSQL 17** (banco único, um schema por serviço) |
| Mensageria | **Confluent Kafka 7.9** (KRaft single-node, sem Zookeeper) |
| Cache | **Redis 7** (cache de taxas de câmbio com TTL) |
| Migrações | **Flyway 12** |
| Testes | **JUnit 5 + Mockito**, cobertura **JaCoCo ≥ 80%** |
| Qualidade | **Spotless** (formatação/lint) + **Husky** hooks |
| Observabilidade | **OpenTelemetry** (traces → Jaeger), **Prometheus + Grafana** (métricas) |
| Infra | **Docker Compose** único (8 containers + perfil observabilidade) |
| CI/CD | **GitHub Actions** (build, testes, cobertura, lint, validação do compose) |
| Frontend | **React** (em desenvolvimento — ver [Roadmap](#-roadmap)) |

> ⚠️ **Nota Boot 4.1:** o Spring Boot 4 extraiu vários auto-configurations para módulos separados. Este projeto usa `spring-boot-starter-flyway`, `spring-boot-starter-kafka` e `spring-boot-restclient` (detalhes nos [ADRs](docs/ADRs.md)).

---

## 🏛️ Arquitetura

```
                    ┌─────────────────────────────────────────────┐
                    │              Cliente / API                   │
                    │        (curl · Postman · futuro React)       │
                    └──────────────────────┬──────────────────────┘
                                           │ HTTP :8080
                                    ┌──────▼──────┐
                                    │   Gateway   │  Spring Cloud Gateway 5
                                    │   :8080     │  JWT validation + CorrelationId
                                    └──────┬──────┘
        ┌───────────────┬──────────────────┼───────────────────┬───────────────┐
        │               │                  │                   │               │
   ┌────▼─────┐   ┌─────▼──────┐    ┌──────▼──────┐    ┌───────▼──────┐   ┌─────▼────────┐
   │   Auth   │   │  Currency  │    │   Credit    │    │  Analytics   │   │              │
   │  :8081   │   │  :8082     │    │  :8083      │    │  :8084       │   │              │
   │ JWT/RBAC │   │ FX rates   │───▶│ Pricing     │───▶│ CQRS read    │   │              │
   │          │   │ + Redis    │    │ Settlement  │    │ projections  │   │              │
   └────┬─────┘   └─────┬──────┘    └──────┬──────┘    └──────┬───────┘   │              │
        │               │                  │                   │           │              │
        └───────┬───────┴──────────────────┼───────────────────┴───────────┘              │
                │                          │                                              │
        ┌───────▼───────┐          ┌───────▼───────┐                            ┌─────────▼─────────┐
        │  PostgreSQL 17 │          │  Kafka 7.9    │                            │ Jaeger · Prometheus│
        │  (1 DB · N     │          │  (settlement  │                            │ · Grafana (perfil  │
        │   schemas)     │          │   .events)    │                            │  observability)    │
        └───────────────┘          └───────────────┘                            └───────────────────┘
```

### Decisões-chave

- **Microserviços** por domínio (`auth`, `currency`, `credit`, `analytics`) atrás de **um gateway** — deploy independente, fronteiras de domínio claras.
- **Banco único PostgreSQL com um schema por serviço** — simplicidade operacional (1 compose), isolamento lógico via schema. [ADR-003](docs/ADRs.md#adr-003-banco-único-com-schema-por-serviço).
- **Event-driven + CQRS no analytics** — a liquidação publica `SettlementEvent` no Kafka; o analytics projeta as tabelas de leitura (`settlement_projection`, `settlement_daily_summary`). Relatórios não tocam o banco transacional. [ADR-002](docs/ADRs.md#adr-002-cqrs-evento--projeção-de-leitura).
- **CorrelationId obrigatório** — um `X-Correlation-Id` nasce no gateway (ou é aceito do cliente) e trafega em toda a cadeia (logs, HTTP, eventos). [Ver `CorrelationIdWebFilter`](backend/gateway-service/src/main/java/com/srm/gateway/filter/CorrelationIdWebFilter.java).
- **JWT/RBAC no gateway** — o gateway valida o token (via `ReactiveJwtDecoder`) e injeta headers `X-User-*` nos serviços downstream.
- **Money = `BigDecimal`** sempre; taxas com 4+ casas decimais. [ADR-004](docs/ADRs.md#adr-004-precisão-decimal-para-dinheiro).
- **Strategy Pattern** no motor de precificação: cada tipo de recebível carrega seu spread (`Duplicata Mercantil 1,5% a.m.`, `Cheque Pré-datado 2,5% a.m.`). [Ver `PricingCalculator`](backend/credit-service/src/main/java/com/srm/credit/pricing/PricingCalculator.java).
- **ACID + optimistic locking** na liquidação (versão na entidade; conflito → `409`). [ADR-005](docs/ADRs.md#adr-005-liquidação-acid-com-optimistic-locking).

---

## 📁 Estrutura do repositório

```
.
├── backend/
│   ├── pom.xml                     # parent Maven (Java 25, Boot 4.1, JaCoCo 80%, Spotless)
│   ├── Dockerfile                  # multi-stage: 1 imagem por serviço (targets)
│   ├── srm-common/                 # lib compartilhada: CorrelationId, error handling, SettlementEvent
│   ├── auth-service/               # usuários, roles, JWT (Flyway: schema auth)
│   ├── gateway-service/            # Spring Cloud Gateway 5: rotas, JWT, CORS, CorrelationId
│   ├── currency-service/           # moedas + taxas de câmbio (Redis cache, Kafka publisher)
│   ├── credit-service/             # recebíveis, tipos, precificação (Strategy), liquidação (ACID)
│   └── analytics-service/          # CQRS: consumer Kafka → projeções → extrato/resumo
├── deploy/
│   ├── prometheus/prometheus.yml   # scrape dos serviços
│   └── grafana/provisioning/       # datasource automático
├── scripts/
│   ├── e2e-smoke.sh                # smoke test E2E do fluxo completo (10/10)
│   └── check-backend.sh            # mvn via Docker (JDK 25) — usado pelo pre-push
├── docker-compose.yml              # sistema inteiro (8 containers + perfil observability)
├── .github/workflows/ci.yml        # CI: backend + frontend + validação do compose
├── .husky/                         # pre-commit (Spotless) e pre-push (testes + cobertura)
├── README_case_dev_srm.md          # enunciado do desafio
└── SRM_Credit_Engine_Specification.md  # spec técnica detalhada
```

---

## 🚀 Como rodar (Docker Compose)

**Pré-requisitos:** Docker + Docker Compose v2 (Windows/macOS/Linux).

```bash
# 1. Configure as credenciais (opcional — defaults apenas para dev)
cp .env.example .env

# 2. Suba o sistema inteiro (build + healthchecks)
docker compose up -d --build

# 3. Acompanhe a subida (todos devem ficar "healthy")
docker compose ps

# 4. Smoke test E2E (login → taxa → simulação → lote → liquidação → extrato)
./scripts/e2e-smoke.sh

# 5. Logs de um serviço
docker compose logs -f credit-service

# 6. Para derrubar (com volumes: docker compose down -v)
docker compose down
```

| Serviço | Porta | Healthcheck |
|---|---|---|
| gateway-service | **8080** (entrada da API) | actuator |
| auth-service | 8081 | actuator |
| currency-service | 8082 | actuator |
| credit-service | 8083 | actuator |
| analytics-service | 8084 | actuator |
| postgres | 5432 | `pg_isready` |
| redis | 6379 | `redis-cli ping` |
| kafka | 9092 (host) / 29092 (interna) | `cub kafka-ready` |

> A primeira subida baixa as imagens e dependências Maven (pode demorar alguns minutos).

---

## 🔑 Credenciais de demonstração

Semeadas automaticamente pelo `auth-service` na primeira subida (variáveis `SEED_*` no compose):

| Usuário | Senha | Role |
|---|---|---|
| `admin` | `Admin@123` | ADMIN |
| `manager` | `Manager@123` | MANAGER (pode registrar taxas de câmbio) |
| `operator` | `Operator@123` | OPERATOR |

> ⚠️ **Apenas para desenvolvimento.** Em produção, defina no `.env` senhas fortes e um `JWT_SECRET` novo (o fallback do compose é um valor conhecido), além de `POSTGRES_PASSWORD`.

---

## 📊 Observabilidade

Perfil opcional do compose:

```bash
docker compose --profile observability up -d --build
```

| Ferramenta | URL | O quê |
|---|---|---|
| Jaeger | http://localhost:16686 | traces distribuídos (OTLP via OpenTelemetry agent) |
| Prometheus | http://localhost:9090 | métricas (`/actuator/prometheus`) |
| Grafana | http://localhost:3000 | dashboards (admin / senha em `GRAFANA_ADMIN_PASSWORD`) |

Para habilitar o agente OpenTelemetry: `OTEL_JAVAAGENT_ENABLED=true` no `.env`.

---

## 🔌 API — referência e exemplos

Todas as rotas passam pelo **gateway (`:8080`)** e exigem `Authorization: Bearer <token>` (exceto `login`).

| Método | Rota | Serviço | Descrição |
|---|---|---|---|
| POST | `/api/v1/auth/login` | auth | Autentica e retorna JWT |
| POST | `/api/v1/auth/users` | auth | Cria usuário (ADMIN) |
| GET | `/api/v1/auth/me` | auth | Usuário corrente |
| GET | `/api/v1/currencies` | currency | Moedas suportadas |
| GET | `/api/v1/exchange-rates` | currency | Taxas de câmbio |
| POST | `/api/v1/exchange-rates` | currency | Registra taxa (MANAGER) — RF01 |
| GET | `/api/v1/receivable-types` | credit | Tipos de recebível e spreads |
| POST | `/api/v1/receivables/price` | credit | **Simulação** de precificação — RF02 |
| POST | `/api/v1/receivables` | credit | Lote de recebíveis — RF02 |
| POST | `/api/v1/receivables/{id}/settle` | credit | **Liquidação** (ACID) — RF03/RF04 |
| GET | `/api/v1/receivables/{id}` | credit | Detalhe do recebível |
| GET | `/api/v1/transactions` | analytics | **Extrato** de liquidações (CQRS) — RF05 |
| GET | `/api/v1/analytics/summary` | analytics | Resumo diário (dashboard) |

### Exemplos

```bash
BASE=http://localhost:8080

# 1. Login
TOKEN=$(curl -s $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"manager","password":"Manager@123"}' | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# 2. Simulação: R$ 100.000 em 90 dias, Duplicata Mercantil (spread 1,5% a.m. + base 0,5% a.m.)
curl -s $BASE/api/v1/receivables/price -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"faceValue":100000,"dueDate":"2026-11-11","receivableTypeId":"<TYPE_ID>",
       "currency":"BRL","settlementCurrency":"BRL","baseRate":0.005}'
# → presentValue ≈ 94232.23   (100000 / (1,02)^3)

# 3. Liquidação cross-currency: recebível USD liquidado em BRL
curl -s -X POST $BASE/api/v1/receivables/<RECEIVABLE_ID>/settle \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"settlementCurrency":"BRL"}'
# → exchangeRateApplied, presentValue (USD) e presentValueInSettlementCurrency (BRL)

# 4. Extrato de liquidações (CQRS — aguarde ~3s o evento Kafka)
curl -s "$BASE/api/v1/transactions?startDate=2026-08-13&endDate=2026-11-11" \
  -H "Authorization: Bearer $TOKEN"
```

**Erros:** resposta padronizada com `requestId` (CorrelationId), `status`, `message` e `timestamp`; validação → `400`, não autorizado → `401`, proibido → `403`, conflito de versão (locking) → `409`.

### RBAC dinâmico no gateway (sem restart)

- As regras de autorização `rota + método + roles` são externas em `backend/gateway-service/src/main/resources/application.yml` no bloco `app.rbac.rules`.
- Cada regra suporta: `id`, `method` (`*` para qualquer método), `path-pattern` (`/api/admin/**`), `roles`, `permit-all`, `priority` e `enabled`.
- O gateway usa *default deny* (`app.rbac.default-deny=true`): se nenhuma regra casar, a requisição é negada.
- O endpoint `POST /actuator/refresh-rbac` deve permanecer protegido por role administrativa (`ADMIN`) na própria matriz de regras.
- Para atualizar regras em runtime, altere a configuração externa e dispare:

```bash
curl -X POST http://localhost:8080/actuator/refresh-rbac \
  -H "Authorization: ******"
```

Resposta inclui `refreshed`, `rulesCount` e `version` do snapshot ativo.

---

## 🗄️ Modelo de dados (ER)

Banco único `srm_credit_engine`, um schema por domínio:

```
auth                              currency
┌───────────────┐                 ┌───────────────────┐       ┌───────────────────────┐
│ users         │                 │ currency          │       │ exchange_rate         │
│ id UUID PK    │                 │ id UUID PK        │       │ id UUID PK            │
│ username UNIQ │                 │ code VARCHAR(3)   │◄──────┤ from_currency_id FK   │
│ password_hash │                 │ name VARCHAR      │       │ to_currency_id FK     │
│ role ENUM     │                 │ active BOOL       │       │ rate DECIMAL(18,6)    │
│ enabled BOOL  │                 └───────────────────┘       │ effective_date DATE   │
└───────────────┘                                             │ unique(pair, date)    │
                                                              └───────────────────────┘
credit
┌───────────────────────────┐     ┌──────────────────────────────────────────────────┐
│ receivable_type           │     │ receivable                                        │
│ id UUID PK                │     │ id UUID PK · version INT (optimistic lock)        │
│ name VARCHAR              │     │ cedente_id UUID (indexado)                        │
│ spread DECIMAL(18,6)      │     │ receivable_type_id FK                             │
│ days_in_month INT         │     │ face_value DECIMAL(18,2)                          │
│ active BOOL               │     │ due_date DATE (indexado) · currency_id FK         │
└───────────────────────────┘     │ status ENUM (indexado)                            │
                                  └───────────────────┬──────────────────────────────┘
                                                      │ receivable_id
                              ┌───────────────────────▼──────────────────────────┐
                              │ transaction (liquidação)                          │
                              │ id UUID PK · version INT                          │
                              │ receivable_id FK (indexado)                       │
                              │ present_value · discount_value DECIMAL(18,2)      │
                              │ currency · settlement_currency VARCHAR(3)         │
                              │ exchange_rate_applied DECIMAL(18,6)               │
                              │ status ENUM (indexado)                            │
                              │ settled_at TIMESTAMPTZ                            │
                              │ index(settlement_currency, settled_at DESC)       │
                              └───────────────────────────────────────────────────┘

analytics (CQRS — alimentado por SettlementEvent do Kafka)
┌───────────────────────────────────┐    ┌───────────────────────────────────────┐
│ settlement_projection             │    │ settlement_daily_summary              │
│ transaction_id UUID PK            │    │ summary_date DATE                     │
│ receivable_id · cedente_id        │    │ currency VARCHAR(3)                   │
│ face_value · present_value        │    │ total_transactions BIGINT             │
│ discount_value DECIMAL(18,2)      │    │ total_present_value DECIMAL(18,2)     │
│ currency · settlement_currency    │    │ total_discount_value DECIMAL(18,2)    │
│ exchange_rate_applied             │    │ PK(summary_date, currency)            │
│ status · settled_at (indexado)    │    └───────────────────────────────────────┘
└───────────────────────────────────┘

DDL versionado: backend/<serviço>/src/main/resources/db/migration/V1__*.sql (Flyway)
```

---

## ✅ Testes, qualidade e CI/CD

### Local (hooks Husky)

- **pre-commit:** `mvn spotless:check` (backend) + lint/typecheck (frontend, se existir).
- **pre-push:** `./scripts/check-backend.sh verify` — testes + cobertura **JaCoCo ≥ 80%** em container JDK 25.

### Verificação manual

```bash
./scripts/check-backend.sh verify      # testes + cobertura + empacotamento (Docker, JDK 25)
```

Resultado atual (14/08/2026):

| Módulo | Testes | Cobertura de linhas |
|---|---|---|
| srm-common | ✅ | 83,0% |
| auth-service | ✅ | 91,0% |
| currency-service | ✅ | 84,9% |
| credit-service | ✅ | 84,6% |
| analytics-service | ✅ | 97,7% |

### CI (GitHub Actions — `.github/workflows/ci.yml`)

1. **Backend:** JDK 25 → `mvn verify` (testes + JaCoCo 80%) → `spotless:check` → upload do relatório.
2. **Frontend:** lint → typecheck → build (habilitado quando o frontend existir).
3. **Compose:** `docker compose config --quiet`.

---

## 📐 Decisões de arquitetura (ADRs)

Documentadas em [docs/ADRs.md](docs/ADRs.md):

- **ADR-001** Microserviços por domínio com API Gateway
- **ADR-002** CQRS: evento `SettlementEvent` (Kafka) → projeção de leitura
- **ADR-003** Banco único PostgreSQL com schema por serviço
- **ADR-004** Precisão decimal (`BigDecimal`) para dinheiro
- **ADR-005** Liquidação ACID com optimistic locking
- **ADR-006** Strategy Pattern no motor de precificação
- **ADR-007** Spring Boot 4: auto-configurations extraídos (Flyway, Kafka, RestClient, Jackson 3)

---

## 🌿 Estratégia de Git

**GitHub Flow** (branches `feature/*` → PR para `main`), com **Conventional Commits** (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`).

```bash
git checkout -b feature/credit-service
git commit -m "feat(credit): adiciona liquidação ACID com optimistic locking"
# PR para main → CI valida → merge
```

**Por quê:** projeto single-repo com entrega contínua por PRs; o GitHub Flow equilibra simplicidade e rastreabilidade (sem a cerimônia do Git Flow), e a `main` é sempre deployável — validada por CI + hooks.

---

## 🚀 Design para alta escala (1M transações/min)

Evolução incremental a partir desta base (detalhes nos ADRs e na spec):

- **Gateway** — escala horizontal atrás de load balancer; rate limiting (Redis) e circuit breaker por rota.
- **PostgreSQL** — *read replicas* para analytics + *connection pooling* (PGBouncer); particionamento por data em `transaction` e `settlement_projection`.
- **Kafka** — cluster multi-broker, *partitions* por `cedente_id` (garante ordenação por cedente), *idempotent producer*.
- **Cache** — Redis para taxas FX e perfis de risco; cache de leitura no analytics.
- **Consistência eventual** — o caminho de liquidação permanece ACID/forte; o analytics tolera latência de segundos (evento → projeção).
- **Observabilidade** — traces (Jaeger/OTel) + métricas (Prometheus) + logs estruturados com `correlationId` para rastreio ponta a ponta.

---

## 🗺️ Roadmap

- [x] Backend: 5 microserviços + gateway (RF01–RF05)
- [x] Infra: Docker Compose completo, Kafka KRaft, Redis, observabilidade (Jaeger/Prometheus/Grafana)
- [x] Testes: JUnit5/Mockito, cobertura ≥ 80%, hooks, CI
- [x] Smoke test E2E do fluxo completo (10/10)
- [ ] **Frontend React** (painel do operador com simulação em tempo real + grid de transações) — o CI já possui o job preparado

---

## 📄 Licença

MIT — ver [LICENSE](LICENSE).
