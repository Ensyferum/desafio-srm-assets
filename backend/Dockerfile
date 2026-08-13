# syntax=docker/dockerfile:1.4
# ============================================================
# SRM Credit Engine — Dockerfile único (multi-stage / multi-target)
#
# Estratégia:
#   1. build — compila TODOS os módulos do backend (Java 25 / Spring Boot 4)
#      uma única vez e gera os JARs executáveis;
#   2. base — imagem fina de runtime (JRE 25 + curl para healthcheck
#      + agente OpenTelemetry opcional);
#   3. <serviço> — cópia o JAR do serviço correspondente.
#
# Uso no docker-compose:
#   build:
#     context: .
#     dockerfile: backend/Dockerfile
#     target: auth-service | gateway-service | currency-service | credit-service | analytics-service
# ============================================================

# ------------------------------------------------------------------
# Estágio de build — compila o reator Maven inteiro
# ------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY backend ./backend
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -f backend/pom.xml package \
        -DskipTests \
        -Dmaven.test.skip=true \
        -Dspotless.check.skip=true \
        -Djacoco.skip=true \
        -Dstyle.color=never

# ------------------------------------------------------------------
# Base de runtime — JRE 25 + curl (healthcheck) + agente OTel opcional
# ------------------------------------------------------------------
FROM eclipse-temurin:25-jre AS base
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ARG OTEL_AGENT_VERSION=v2.14.0
RUN mkdir -p /opt/otel \
    && (curl -fsSL -o /opt/otel/opentelemetry-javaagent.jar \
         "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar" \
         || echo "AVISO: agente OTel indisponível — observabilidade desativada") \
    && ls -la /opt/otel/

ENV OTEL_JAVAAGENT_ENABLED=false

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=15 \
    CMD curl -fsS http://localhost:8080/actuator/health/readiness | grep -q '"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "if [ \"$OTEL_JAVAAGENT_ENABLED\" = \"true\" ] && [ -f /opt/otel/opentelemetry-javaagent.jar ]; then exec java -javaagent:/opt/otel/opentelemetry-javaagent.jar -jar /app/app.jar; else exec java -jar /app/app.jar; fi"]

# ------------------------------------------------------------------
# Imagens por serviço
# ------------------------------------------------------------------
FROM base AS auth-service
COPY --from=build /workspace/backend/auth-service/target/auth-service-1.0.0.jar /app/app.jar

FROM base AS gateway-service
COPY --from=build /workspace/backend/gateway-service/target/gateway-service-1.0.0.jar /app/app.jar

FROM base AS currency-service
COPY --from=build /workspace/backend/currency-service/target/currency-service-1.0.0.jar /app/app.jar

FROM base AS credit-service
COPY --from=build /workspace/backend/credit-service/target/credit-service-1.0.0.jar /app/app.jar

FROM base AS analytics-service
COPY --from=build /workspace/backend/analytics-service/target/analytics-service-1.0.0.jar /app/app.jar
