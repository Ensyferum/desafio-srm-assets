# Load Test — k6 (RNF04)

Validação de performance da API (critério: **1000+ req/s** e consultas analíticas
**< 500ms p95**).

## Pré-requisitos

- Stack rodando: `docker compose up -d --build` (com observabilidade, se quiser métricas)
- Gateway acessível em `http://localhost:8080`

## Como rodar

### Opção 1 — Via Docker Compose (recomendado)

O serviço `k6` faz parte do compose com o perfil `loadtest` (não sobe por padrão).
Ele roda na rede interna do compose, apontando direto para o gateway
(`http://gateway-service:8080`):

```bash
# Smoke test (rápido — sanidade do fluxo)
docker compose --profile loadtest run --rm k6

# Teste de carga (ramp-up até 100 VUs ≈ 2000 req/s agregado)
docker compose --profile loadtest run --rm k6 k6 run /scripts/load.js
```

### Opção 2 — k6 local contra o host

```bash
# Smoke
k6 run --env BASE_URL=http://localhost:8080 k6/smoke.js

# Carga
k6 run --env BASE_URL=http://localhost:8080 k6/load.js

# Carga mais agressiva (sobrescreve o alvo de VUs)
k6 run --env BASE_URL=http://localhost:8080 --vus 200 --duration 90s k6/load.js
```

## Cenários

| Script      | Objetivo                                                                 |
|-------------|--------------------------------------------------------------------------|
| `smoke.js`  | 1 VU, 5 iterações: login + lista + taxas + extrato + simulação. Sanidade. |
| `load.js`   | Ramp-up 0→50→100→0 VUs, mix de operações (40% lista, 25% extrato, 20% taxas, 10% simulação, 5% liquidação) |

## Interpretação dos resultados

O resumo final do k6 mostra as métricas-chave:

```
http_reqs...............: 2.345,6/s   ← throughput agregado (objetivo: > 1000/s)
http_req_duration.......: avg=42ms  p(95)=180ms   ← latência (objetivo: p95 < 500ms)
http_req_failed.........: 0.00%      ← taxa de erro (objetivo: < 1%)
```

- **Thresholds**: o `load.js` falha (`✗`) se `http_reqs < 100/s`, `p95 > 800ms` ou
  `http_req_failed > 1%`.
- **Observabilidade**: durante o teste, acompanhe no Grafana (dashboard *SRM Credit
  Engine — Visão Geral*) as métricas de HTTP por serviço e as liquidações; no Jaeger,
  os spans do gateway → credit → currency.

## Limitações (ambiente dev)

- Rodando na máquina local, o gargalo costuma ser CPU/network do Docker Desktop, não a
  aplicação. Resultados absolutos servem como *baseline* de regressão, não como prova de
  capacidade produtiva.
- Para um teste mais próximo de produção, considere executar k6 fora do Docker contra um
  deploy em nuvem (CI) e usar `--vus` maiores.
