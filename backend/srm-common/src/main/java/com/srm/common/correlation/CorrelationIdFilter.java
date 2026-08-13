package com.srm.common.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro que garante um {@code X-Correlation-Id} em toda a requisição: reutiliza o header recebido
 * (propagado pelo gateway) ou gera um novo UUID, registra no MDC para logs estruturados e devolve
 * no header da resposta.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(CorrelationIds.HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        CorrelationIds.set(correlationId);
        response.setHeader(CorrelationIds.HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelationIds.clear();
        }
    }
}
