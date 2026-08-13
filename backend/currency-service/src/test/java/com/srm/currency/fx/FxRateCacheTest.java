package com.srm.currency.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.srm.currency.dto.ExchangeRateResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class FxRateCacheTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private FxRateCache cache;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 12);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        cache = new FxRateCache(redis, mapper, 5);
    }

    @Test
    void storesAndReadsRateAsJson() throws Exception {
        ExchangeRateResponse rate =
                new ExchangeRateResponse("USD", "BRL", new BigDecimal("5.4523"), DATE);
        String json =
                new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(rate);
        when(valueOps.get("fx:rate:USD:BRL:2026-08-12")).thenReturn(json);

        Optional<ExchangeRateResponse> result = cache.get("USD", "BRL", DATE);

        assertThat(result).isPresent();
        assertThat(result.get().rate()).isEqualByComparingTo("5.4523");
    }

    @Test
    void returnsEmptyWhenKeyMissing() {
        when(valueOps.get("fx:rate:USD:BRL:2026-08-12")).thenReturn(null);

        assertThat(cache.get("USD", "BRL", DATE)).isEmpty();
    }

    @Test
    void returnsEmptyAndDoesNotThrowWhenRedisFails() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));

        assertThat(cache.get("USD", "BRL", DATE)).isEmpty();
    }

    @Test
    void evictsKey() {
        cache.evict("USD", "BRL", DATE);
        verify(redis).delete("fx:rate:USD:BRL:2026-08-12");
    }

    @Test
    void evictDoesNotThrowWhenRedisFails() {
        when(redis.delete(anyString())).thenThrow(new RuntimeException("redis down"));
        cache.evict("USD", "BRL", DATE);
        assertThat(cache).isNotNull();
    }
}
