package com.srm.common.correlation;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Propaga o correlation id (do MDC) para chamadas HTTP entre serviços, garantindo rastreabilidade
 * fim-a-fim.
 */
public class CorrelationIdClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String correlationId = CorrelationIds.get();
        if (correlationId != null && request.getHeaders().getFirst(CorrelationIds.HEADER) == null) {
            request.getHeaders().set(CorrelationIds.HEADER, correlationId);
        }
        return execution.execute(request, body);
    }
}
