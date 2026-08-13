package com.srm.credit.pricing;

import com.srm.credit.domain.ReceivableType;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Estratégia padrão: taxa base + spread configurável do tipo (fallback). */
@Component
public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public String supports() {
        return null;
    }

    @Override
    public BigDecimal effectiveMonthlyRate(
            ReceivableType type, BigDecimal baseRate, BigDecimal termMonths) {
        return baseRate.add(type.getSpreadMonthly());
    }
}
