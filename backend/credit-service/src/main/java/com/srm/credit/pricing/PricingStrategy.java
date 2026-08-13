package com.srm.credit.pricing;

import com.srm.credit.domain.ReceivableType;
import java.math.BigDecimal;

/**
 * Estratégia de precificação por tipo de recebível (RF02 — Strategy Pattern).
 *
 * <p>Cada estratégia define a taxa mensal efetiva aplicada na fórmula: {@code Valor Presente =
 * Valor Face / (1 + taxa)^prazo}.
 */
public interface PricingStrategy {

    /**
     * Nome do tipo de recebível suportado por esta estratégia. Retorna {@code null} para a
     * estratégia padrão (fallback).
     */
    String supports();

    /**
     * Taxa mensal efetiva (taxa base + spread do tipo + prêmio de risco da estratégia).
     *
     * @param type tipo de recebível (carrega o spread mensal configurável)
     * @param baseRate taxa base de mercado (ex.: Selic meta)
     * @param termMonths prazo em meses (dias / 30)
     */
    BigDecimal effectiveMonthlyRate(
            ReceivableType type, BigDecimal baseRate, BigDecimal termMonths);
}
