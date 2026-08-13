package com.srm.common.correlation;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * Utilitário do Correlation ID — identificador único que trafega durante toda a solicitação (HTTP →
 * serviços → Kafka), permitindo rastreabilidade fim-a-fim.
 *
 * <p>O valor vive no MDC do SLF4J, por isso aparece em todos os logs estruturados.
 */
public final class CorrelationIds {

    /** Header HTTP usado para propagar o correlation id entre serviços. */
    public static final String HEADER = "X-Correlation-Id";

    /** Chave usada no MDC dos logs estruturados. */
    public static final String MDC_KEY = "correlationId";

    private static final int MAX_LENGTH = 64;

    private CorrelationIds() {}

    /** Retorna o correlation id atual (ou {@code null} se não houver). */
    public static String get() {
        String value = MDC.get(MDC_KEY);
        return (value == null || value.isBlank()) ? null : value;
    }

    /** Retorna o correlation id atual ou gera um novo UUID. */
    public static String getOrCreate() {
        String current = get();
        if (current == null) {
            current = UUID.randomUUID().toString();
            set(current);
        }
        return current;
    }

    /** Define o correlation id no MDC, sanitizando tamanho para evitar log injection. */
    public static void set(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return;
        }
        String sanitized =
                correlationId.length() > MAX_LENGTH
                        ? correlationId.substring(0, MAX_LENGTH)
                        : correlationId;
        MDC.put(MDC_KEY, sanitized);
    }

    /** Remove o correlation id do MDC (limpeza de contexto por thread). */
    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
