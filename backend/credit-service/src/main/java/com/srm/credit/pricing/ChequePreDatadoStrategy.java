package com.srm.credit.pricing;

import com.srm.credit.domain.ReceivableType;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Estratégia para cheques pré-datados: além do spread configurável, aplica um prêmio de risco
 * adicional de 0,5 p.p. a.m. para prazos acima de 6 meses (maior exposição a
 * devolução/insuficiência de fundos).
 */
@Component
public class ChequePreDatadoStrategy implements PricingStrategy {

    public static final String TYPE_NAME = "Cheque Pré-datado";
    private static final BigDecimal LONG_TERM_THRESHOLD = new BigDecimal("6");
    private static final BigDecimal EXTRA_RISK_PREMIUM = new BigDecimal("0.005");

    @Override
    public String supports() {
        return TYPE_NAME;
    }

    @Override
    public BigDecimal effectiveMonthlyRate(
            ReceivableType type, BigDecimal baseRate, BigDecimal termMonths) {
        BigDecimal rate = baseRate.add(type.getSpreadMonthly());
        if (termMonths.compareTo(LONG_TERM_THRESHOLD) > 0) {
            rate = rate.add(EXTRA_RISK_PREMIUM);
        }
        return rate;
    }
}
