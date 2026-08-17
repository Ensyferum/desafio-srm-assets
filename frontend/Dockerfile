# ============================================================
# Frontend React (Vite) — multi-stage: build → nginx
# Uso via docker-compose (build context = raiz do repo):
#   dockerfile: frontend/Dockerfile
# ============================================================

# ---------- Stage 1: build ----------
FROM node:22-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---------- Stage 2: serve ----------
FROM nginx:1.27-alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
# Usa 127.0.0.1: `localhost` pode resolver para ::1 (IPv6) e o nginx escuta só em IPv4.
HEALTHCHECK --interval=10s --timeout=3s --retries=5 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null 2>&1 || exit 1
