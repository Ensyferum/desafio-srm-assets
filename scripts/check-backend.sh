#!/usr/bin/env bash
# ============================================================
# Executa comandos Maven do backend dentro de um container
# com JDK 25 (imagem maven:3.9-eclipse-temurin-25).
#
# Uso:  ./scripts/check-backend.sh <goal...>   (ex: verify)
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
M2_CACHE="${M2_CACHE:-$HOME/.m2}"

# MSYS_NO_PATHCONV evita que o Git Bash converta /workspace em caminho Windows
MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$ROOT:/workspace" \
  -v "$M2_CACHE:/root/.m2" \
  -w /workspace/backend \
  maven:3.9-eclipse-temurin-25 \
  mvn -B -q "$@"
