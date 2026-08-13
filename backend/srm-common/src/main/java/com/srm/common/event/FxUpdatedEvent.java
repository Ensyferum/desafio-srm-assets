package com.srm.common.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento publicado no tópico {@code fx.updated} quando uma taxa de câmbio é criada/atualizada —
 * permite que outros serviços invalidem caches ou registrem o histórico.
 */
public record FxUpdatedEvent(
        String fromCurrency,
        String toCurrency,
        BigDecimal rate,
        LocalDate effectiveDate,
        String createdBy) {}
