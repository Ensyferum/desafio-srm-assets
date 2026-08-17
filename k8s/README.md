# Kubernetes — SRM Credit Engine (Seção 9.2)

Manifests de produção para a plataforma. O **docker-compose** continua sendo o
ambiente padrão de desenvolvimento; estes manifests permitem subir a mesma
arquitetura em um cluster (kind, minikube, EKS, AKS, GKE, etc.).

## Arquivos

| Arquivo              | Conteúdo                                                        |
|----------------------|-----------------------------------------------------------------|
| `namespace.yaml`     | Namespace `srm-credit-engine`                                   |
| `configmap.yaml`     | Variáveis de ambiente não-secretas (DB URL, Kafka, OTel, URIs)  |
| `secret.example.yaml`| Secret com senhas/credenciais (ajuste os valores)               |
| `infra.yaml`         | Postgres (StatefulSet), Redis e Kafka (KRaft single-node)       |
| `deployments.yaml`   | 5 microserviços + frontend, com liveness/readiness probes       |
| `services.yaml`      | Services ClusterIP                                             |
| `ingress.yaml`       | Ingress (frontend em `/`, API e Swagger via gateway)            |
| `kustomization.yaml` | Aplica tudo com `kubectl apply -k k8s/`                        |

## Pré-requisitos

- Cluster Kubernetes (testado com kind/minikube) + `kubectl`
- Imagens construídas e disponíveis no cluster (ver abaixo)
- (Opcional) Ingress Controller (nginx) e coletor OTel para tracing

## Como usar

```bash
# 1. (Opcional) Build das imagens e carga no cluster
docker build -f backend/Dockerfile --target auth-service -t srm/auth-service:1.0.0 .
docker build -f backend/Dockerfile --target gateway-service -t srm/gateway-service:1.0.0 .
docker build -f backend/Dockerfile --target currency-service -t srm/currency-service:1.0.0 .
docker build -f backend/Dockerfile --target credit-service -t srm/credit-service:1.0.0 .
docker build -f backend/Dockerfile --target analytics-service -t srm/analytics-service:1.0.0 .
docker build -f frontend/Dockerfile -t srm/frontend:1.0.0 .
kind load docker-image srm/auth-service:1.0.0 srm/gateway-service:1.0.0 \
  srm/currency-service:1.0.0 srm/credit-service:1.0.0 srm/analytics-service:1.0.0 \
  srm/frontend:1.0.0

# 2. Ajustar segredos (nunca commitar valores reais)
cp k8s/secret.example.yaml k8s/secret.yaml
#   edite os valores e troque a referência em kustomization.yaml

# 3. Aplicar o ambiente
kubectl apply -k k8s/

# 4. Acompanhar
kubectl -n srm-credit-engine get pods -w
```

## Acessos

| Recurso          | URL (via Ingress, host `srm.local`)        |
|------------------|---------------------------------------------|
| Frontend         | http://srm.local                            |
| API (gateway)    | http://srm.local/api/v1/...                 |
| Swagger agregado | http://srm.local/swagger/{service}/swagger-ui.html |

Sem Ingress, use port-forward:

```bash
kubectl -n srm-credit-engine port-forward svc/gateway-service 8080:8080
kubectl -n srm-credit-engine port-forward svc/frontend 3000:80
```

## Observabilidade

- Métricas: `/actuator/prometheus` exposto em cada serviço — adicione um
  `ServiceMonitor` (Prometheus Operator) apontando para os Services.
- Tracing: o `configmap.yaml` aponta `OTEL_EXPORTER_OTLP_ENDPOINT` para
  `http://otel-collector:4318` — implante um coletor OpenTelemetry (ou ajuste
  o endpoint para o seu backend de tracing).
- Health: probes de liveness/readiness em `/actuator/health/liveness` e
  `/actuator/health/readiness` (mesmos paths do docker-compose).

## Notas

- **Banco**: os microserviços usam o mesmo PostgreSQL (schema por serviço via
  Flyway). Em produção, substitua o `StatefulSet` por um serviço gerenciado e
  atualize `SPRING_DATASOURCE_URL` no `configmap.yaml`.
- **Kafka**: KRaft single-node igual ao compose; em produção, use MSK/Confluent
  e atualize `KAFKA_BOOTSTRAP_SERVERS`.
- **Escalabilidade (RNF04)**: aumente `replicas` dos Deployments para escalar
  horizontalmente (serviços são stateless; o estado vive no Postgres/Redis/Kafka).
