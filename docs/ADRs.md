# Architecture Decision Records (ADRs)

Decisões de arquitetura do **SRM Credit Engine**, no formato ADR (Status · Contexto · Decisão · Consequências).

---

## ADR-001 — Microserviços por domínio com API Gateway

**Status:** Aceito

**Contexto:** O domínio cobre autenticação, câmbio, precificação/liquidação e analytics, com requisitos de independência de deploy e fronteiras claras. A especificação pede "arquitetura orientada a serviços".

**Decisão:** Quatro serviços (`auth`, `currency`, `credit`, `analytics`) + `gateway` (Spring Cloud Gateway 5), todos no mesmo repositório, orquestrados por um único `docker-compose.yml`. Um módulo compartilhado (`srm-common`) para CorrelationId, tratamento de erro e contrato de eventos.

**Consequências:** + Independência de deploy e escalonamento por domínio; fronteiras de domínio explícitas. − Custo operacional (5 JVMs), complexidade de integração (resolvida por gateway + eventos). Para o porte do desafio, o compose único mantém a simplicidade ("subir o sistema inteiro com 1 comando").

---

## ADR-002 — CQRS: evento `SettlementEvent` (Kafka) → projeção de leitura

**Status:** Aceito

**Contexto:** O extrato de liquidações (RF05) precisa filtrar grandes volumes por período/cedente/moeda **sem** competir com o banco transacional da liquidação.

**Decisão:** A liquidação (credit-service) persiste a transação em transação local **e** publica `SettlementEvent` no tópico `settlement.events`. O analytics-service consome e projeta `settlement_projection` (extrato) e `settlement_daily_summary` (resumo do dashboard), consultadas por JDBC nativo com índices dedicados — sem ORM para relatórios.

**Consequências:** + Leitura analítica isolada do caminho ACID; relatórios otimizados (2 camadas: controller + repositório). − Consistência eventual (segundos) entre liquidação e extrato — aceitável e documentado; a liquidação em si permanece ACID.

---

## ADR-003 — Banco único PostgreSQL com schema por serviço

**Status:** Aceito

**Contexto:** A SPEC pede "Banco de Dados único relacional PostgreSQL" e "todos os componentes no Docker Compose". Microserviços clássicos defendem um banco por serviço.

**Decisão:** Um banco `srm_credit_engine`, com **um schema por domínio** (`auth`, `currency`, `credit`, `analytics`), criado por Flyway dentro de cada serviço. Nenhum serviço acessa schema de outro.

**Consequências:** + Um único Postgres no compose (simplicidade operacional); isolamento lógico por schema + credenciais; migrations versionadas por serviço. − Não permite escalar bancos separadamente; se um dia necessário, a separação é mecânica (basta apontar o datasource para outro banco e repetir a migration).

---

## ADR-004 — Precisão decimal para dinheiro

**Status:** Aceito

**Contexto:** Cálculo financeiro (valor presente, deságio, conversão cambial) não pode sofrer erro de arredondamento de ponto flutuante.

**Decisão:** Todo valor monetário e taxas usam **`java.math.BigDecimal`** (face value, present value, discount, exchange rates com 4–6 casas). Nunca `double`/`float`. No banco: `DECIMAL(18,2)` para valores e `DECIMAL(18,6)` para taxas.

**Consequências:** + Correção aritmética exigida pelo domínio. − Verbosidade de código (mitigada por helpers de construção nos DTOs).

---

## ADR-005 — Liquidação ACID com optimistic locking

**Status:** Aceito

**Contexto:** "Nenhuma liquidação pode ficar pela metade" — duas liquidações simultâneas do mesmo recebível precisam ser impossíveis.

**Decisão:** A liquidação roda em **transação única** (`@Transactional`) que: (1) re-carrega o recebível com `version` (optimistic lock); (2) valida status; (3) aplica precificação + conversão FX (com retry/fallback no currency-service); (4) cria a transação; (5) marca o recebível `SETTLED`; (6) publica o evento Kafka **dentro** da transação (outbox simplificado: publish com callback/compensação). Conflito de versão → `409 Conflict`.

**Consequências:** + Integridade garantida sob concorrência. − Custo de re-query na transação (aceitável); publicação Kafka dentro da transação adiciona acoplamento — o consumidor (analytics) é idempotente (`ON CONFLICT (transaction_id)`), então reprocessamento é seguro.

---

## ADR-006 — Strategy Pattern no motor de precificação

**Status:** Aceito

**Contexto:** Cada tipo de recebível tem uma regra de risco (spread) diferente, e a especificação pede explicitamente o padrão **Strategy** para desacoplar a regra do cálculo.

**Decisão:** `PricingCalculator` é o orquestrador; cada `ReceivableType` carrega seu `spread` (Duplicata Mercantil 1,5% a.m., Cheque Pré-datado 2,5% a.m.) e a estratégia é resolvida por tipo. Fórmula: `PV = Face / (1 + baseRate + spread)^prazo`; operações cross-currency convertem o PV pela taxa vigente do currency-service no final.

**Consequências:** + Novos tipos de recebível entram sem tocar o cálculo (dados, não código). − As estratégias são parametrizadas por dados; se surgirem fórmulas estruturalmente diferentes, extrai-se a interface `PricingStrategy`.

---

## ADR-007 — Spring Boot 4: auto-configurations extraídos

**Status:** Aceito

**Contexto:** Ao subir com Spring Boot 4.1, várias auto-configurações do Boot 3.x simplesmente não rodavam (Flyway, Kafka, Jackson, RestClient), sem erro de compilação — só em runtime.

**Decisão:** Mapeamento empírico dos módulos e dependências explícitas:

| Capacidade | Módulo necessário (Boot 4.1) |
|---|---|
| Migrações Flyway | `spring-boot-starter-flyway` (inclui `spring-boot-flyway`) |
| Kafka producer/consumer | `spring-boot-starter-kafka` |
| `RestClient.Builder` | `spring-boot-restclient` (traz `http-client` + `http-converter`) |
| JSON serialização | Jackson 3 (`tools.jackson.databind`) — padrão do Boot 4; código migrado de `com.fasterxml.jackson` |
| Gateway routes | Prefixo SCG 5: `spring.cloud.gateway.server.webflux.routes` (antes `spring.cloud.gateway.routes`) |

**Consequências:** + Compatibilidade correta com o ecossistema Boot 4; conhecimento registrado para o time. − Dependências explícitas a mais nos POMs; qualquer upgrade major futuro exige a mesma abordagem de verificação empírica (inspeção de jars/bytecode), nunca suposição.
