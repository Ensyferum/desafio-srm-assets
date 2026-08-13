package com.srm.credit.fx;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Conversão cambial para operações cross-currency (título em BRL, pagamento em USD e vice-versa).
 */
@Service
public class FxConversionService {

    private final FxRateClient fxRateClient;

    public FxConversionService(FxRateClient fxRateClient) {
        this.fxRateClient = fxRateClient;
    }

    /**
     * Retorna a taxa do par {@code from→to} (1 from = rate to), com fallback para a inversão do par
     * oposto quando necessário.
     */
    public BigDecimal getRate(String from, String to, LocalDate date) {
        if (from.equalsIgnoreCase(to)) {
            return BigDecimal.ONE;
        }
        return fxRateClient.fetchRate(from, to, date);
    }
}
