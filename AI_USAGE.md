# AI_USAGE.md — Uso de IA como Co-Pilot

> Documento exigido pela especificação do desafio (seção 2 — *Política de Uso de IA*).
> Autoria intelectual e revisão final: **humana**. A IA foi usada como co-piloto (scaffolding, pesquisa, revisão), com cada decisão validada empiricamente.

---

## 1. Prompts estratégicos utilizados

| Área | Prompt (resumo) | Resultado |
|---|---|---|
| Scaffolding | "Crie o sistema inteiro a partir de SPEC.md e SRM_Credit_Engine_Specification.md" | Estrutura multi-módulo Maven (srm-common + 5 serviços), compose, migrations, testes |
| Compatibilidade | "Spring Boot 4.1 extraiu auto-configurations; descubra onde estão Flyway, Kafka, RestClient e Jackson" | Adição de `spring-boot-starter-flyway`, `spring-boot-starter-kafka`, `spring-boot-restclient`; migração para Jackson 3 (`tools.jackson`) |
| Gateway | "Spring Cloud Gateway 5 não roteia rotas do application.yml" | Descoberta do novo prefixo `spring.cloud.gateway.server.webflux` (via inspeção do bytecode de `GatewayProperties`) |
| Infra | "Kafka cp-kafka 7.9 em KRaft não sobe no compose" | `CLUSTER_ID` literal + listeners com hostname do serviço (não `0.0.0.0`) + healthcheck `cub kafka-ready` no listener interno |
| E2E | "Escreva um smoke test que percorra o fluxo de negócio completo pelo gateway" | `scripts/e2e-smoke.sh` — 10/10 checks verdes |
| Qualidade | "Rode mvn verify e corrija o que falhar" | Testes + cobertura JaCoCo ≥ 80% em todos os módulos |
| Frontend | "Implemente o painel do operador (React) conforme a spec" | React 19 + Vite 6 + TS + Tailwind 4: login, dashboard, simulação em tempo real, recebíveis/liquidação, taxas FX, extrato — validado no compose (:3000) |

## 2. Onde a IA alucinou / gerou código inseguro — e como foi corrigido

| # | Alucinação / erro | Como foi detectado | Correção aplicada |
|---|---|---|---|
| 1 | Assumiu que `spring.flyway.*` ativaria o Flyway no Boot 4.1 (comportamento do Boot 3.x) | Nenhuma migration aplicada; Hibernate falhava com tabelas ausentes | Verificado no jar `spring-boot-autoconfigure-4.1.0.jar`: classe Flyway não existe mais lá. Adicionado `spring-boot-starter-flyway` aos 4 serviços |
| 2 | Usou `uuid-ossp` (`uuid_generate_v4()`) nas migrations | Extensão instalada em schema errado; função não resolvida | Trocado para `gen_random_uuid()` (nativo PG 13+) e removida a dependência da extensão |
| 3 | Injetou Jackson 2 (`com.fasterxml.jackson.databind.ObjectMapper`) | `NoSuchBeanDefinitionException: ObjectMapper` em runtime | Boot 4 usa Jackson 3 por padrão; migrado `FxRateCache` para `tools.jackson.databind` (com teste atualizado) |
| 4 | Assumiu `KafkaTemplate` autoconfigurado | `UnsatisfiedDependencyException` | Autoconfig de Kafka também foi extraído; adicionado `spring-boot-starter-kafka` |
| 5 | Assumiu bean `RestClient.Builder` no `spring-boot-http-client` | Erro persistente mesmo com o módulo no classpath | Inspeção do bytecode revelou que o builder vem de `spring-boot-restclient` (que traz http-client transitivo) |
| 6 | Configurou o gateway com prefixo `spring.cloud.gateway.routes` (SCG 3.x) | Nenhuma rota casava; 404 em tudo | SCG 5.0 mudou o prefixo para `spring.cloud.gateway.server.webflux` — confirmado no constant pool de `GatewayProperties` |
| 7 | Kafka KRaft com `KAFKA_CLUSTER_ID` e listeners `0.0.0.0` | Entrypoint do cp-kafka rejeitava o format | Reproduzido o fluxo `configure → format` em container one-off; corrigido para `CLUSTER_ID` + hostnames no `listeners` |
| 8 | `Instant` passado direto ao JDBC no analytics | `PSQLException` em runtime (coluna `timestamptz`) | Convertido para `java.sql.Timestamp` no repositório; teste atualizado |
| 9 | Nome de campo errado no E2E (`presentValueInSettlement`) | Campo ausente no JSON real | Campo real é `presentValueInSettlementCurrency` — confirmado no DTO e corrigido no script |
| 10 | Frontend: `exchangeRateApplied` tipado como `number` | Typecheck não acusava, mas o backend devolve `null` para moedas iguais (BRL→BRL) | Tipos corrigidos para `number \| null` + guards nos componentes (descoberto na revisão de código, não em runtime) |

> Regra aplicada em todos os casos: **nenhuma correção foi aceita por "confiança" — cada uma foi reproduzida e validada** (jar inspecionado, container one-off, bytecode, respostas reais da API).

## 3. Análise crítica

### Onde a IA economizou tempo ✅
- **Scaffolding inicial** (5 serviços + compose + migrations) em minutos em vez de horas.
- **Diagnóstico de compatibilidade Boot 4** — a IA seguiu a trilha de "autoconfig extraído" de forma sistemática, inspecionando jars e bytecode em vez de tentativa e erro cega.
- **Smoke test E2E** — gerou um script de negócio completo que virou o critério de aceite executável (10/10).
- **Documentação** (este arquivo, ADRs, README) consolidada rapidamente.

### Onde a IA atrapalhou / exigiu atenção ⚠️
- **Conhecimento defasado de versões:** o modelo "pensava" em Spring Boot 3.x / SCG 3.x / Jackson 2. Cada salto de versão major (Boot 4, SCG 5, Jackson 3) gerou falsas suposições que só foram resolvidas com **evidência empírica** (jars, bytecode, logs).
- **Falsas correções em loop:** por duas vezes uma correção foi aplicada com confiança mas não resolvia o problema (ex.: `spring-boot-http-client` vs `spring-boot-restclient`) — o custo foi um rebuild extra. A lição: **verificar o classpath real do artefato antes de editar o POM**.
- **Formatos de output em scripts bash** (extração de campos JSON via `sed`) precisaram de iteração contra a resposta real da API.

### Veredito
A IA foi uma alavanca real de produtividade, **desde que o humano mantivesse o papel de "engenheiro de verificação"**: toda afirmação da IA sobre runtime foi tratada como hipótese e confirmada com evidência antes de virar código definitivo. Sem esse controle, pelo menos 4 dos 9 erros acima teriam ido para produção.
