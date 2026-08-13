package com.srm.currency.fx;

import com.srm.currency.dto.ExchangeRateResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Cache de taxas de câmbio (Redis, TTL padrão 5 min — RNF04).
 *
 * <p>Implementação cache-aside resiliente: qualquer falha do Redis é apenas logada e a consulta
 * segue para o banco (fallback), sem quebrar o fluxo.
 */
@Component
public class FxRateCache {

    private static final Logger log = LoggerFactory.getLogger(FxRateCache.class);
    private static final String KEY_PREFIX = "fx:rate:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public FxRateCache(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${app.fx.cache-ttl-minutes:5}") long cacheTtlMinutes) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofMinutes(cacheTtlMinutes);
    }

    public Optional<ExchangeRateResponse> get(String from, String to, LocalDate date) {
        try {
            String json = redis.opsForValue().get(key(from, to, date));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, ExchangeRateResponse.class));
        } catch (Exception ex) {
            log.warn("Falha ao ler cache Redis (seguindo para o banco): {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public void put(String from, String to, LocalDate date, ExchangeRateResponse rate) {
        try {
            redis.opsForValue()
                    .set(key(from, to, date), objectMapper.writeValueAsString(rate), ttl);
        } catch (Exception ex) {
            log.warn("Falha ao gravar cache Redis: {}", ex.getMessage());
        }
    }

    public void evict(String from, String to, LocalDate date) {
        try {
            redis.delete(key(from, to, date));
        } catch (Exception ex) {
            log.warn("Falha ao invalidar cache Redis: {}", ex.getMessage());
        }
    }

    private String key(String from, String to, LocalDate date) {
        return KEY_PREFIX + from + ":" + to + ":" + date;
    }
}
