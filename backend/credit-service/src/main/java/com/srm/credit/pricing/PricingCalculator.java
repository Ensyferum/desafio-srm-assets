package com.srm.credit.pricing;

import com.srm.common.error.BusinessException;
import com.srm.credit.domain.Receivable;
import com.srm.credit.domain.ReceivableType;
import com.srm.credit.fx.FxConversionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Motor de precificação (RF02):
 *
 * <pre>
 *   Prazo (meses)  = (due_date - as_of_date) / 30
 *   Valor Presente = Valor Face / (1 + Taxa Base + Spread)^Prazo
 *   Se cross-currency: VP(liquidação) = VP(título) / Taxa(liquidação → título)
 * </pre>
 *
 * <p>Todo o dinheiro é manipulado com {@link BigDecimal} e arredondado com {@link
 * RoundingMode#HALF_EVEN}. O expoente fracionário (prazo em dias/30) é o único ponto que usa {@code
 * Math.pow} — necessário para potência não inteira — mas o resultado financeiro final é sempre
 * BigDecimal/HALF_EVEN.
 */
@Component
public class PricingCalculator {

    public static final BigDecimal DEFAULT_BASE_RATE = new BigDecimal("0.005");
    public static final BigDecimal DAYS_PER_MONTH = new BigDecimal("30");
    public static final int MONEY_SCALE = 2;
    public static final int RATE_SCALE = 10;

    private static final BigDecimal ONE = BigDecimal.ONE;

    private final PricingStrategyRegistry strategyRegistry;
    private final FxConversionService fxConversionService;

    public PricingCalculator(
            PricingStrategyRegistry strategyRegistry, FxConversionService fxConversionService) {
        this.strategyRegistry = strategyRegistry;
        this.fxConversionService = fxConversionService;
    }

    /** Simula a precificação de um recebível na data de referência. */
    public PricingResult calculate(
            Receivable receivable,
            BigDecimal baseRate,
            String settlementCurrency,
            LocalDate asOfDate) {
        return calculate(
                receivable.getType(),
                receivable.getFaceValue(),
                receivable.getDueDate(),
                receivable.getCurrency(),
                settlementCurrency,
                baseRate,
                asOfDate);
    }

    public PricingResult calculate(
            ReceivableType type,
            BigDecimal faceValue,
            LocalDate dueDate,
            String currency,
            String settlementCurrency,
            BigDecimal baseRate,
            LocalDate asOfDate) {
        BigDecimal effectiveBase = baseRate == null ? DEFAULT_BASE_RATE : baseRate;
        BigDecimal termMonths = termMonths(asOfDate, dueDate);

        PricingStrategy strategy = strategyRegistry.resolve(type.getName());
        BigDecimal effectiveRate = strategy.effectiveMonthlyRate(type, effectiveBase, termMonths);

        BigDecimal factor = ONE.add(effectiveRate);
        BigDecimal denominator = power(factor, termMonths);
        BigDecimal presentValue =
                faceValue.divide(denominator, MONEY_SCALE, RoundingMode.HALF_EVEN);
        BigDecimal discountValue = faceValue.subtract(presentValue);

        BigDecimal exchangeRateApplied = null;
        BigDecimal presentValueInSettlement = presentValue; // mesma moeda → sem conversão

        if (!currency.equalsIgnoreCase(settlementCurrency)) {
            // Taxa "liquidação → título": se não existir o par direto, inverte o par oposto
            BigDecimal fxRate = fxConversionService.getRate(settlementCurrency, currency, asOfDate);
            exchangeRateApplied = fxRate;
            presentValueInSettlement =
                    presentValue.divide(fxRate, MONEY_SCALE, RoundingMode.HALF_EVEN);
        }

        return new PricingResult(
                faceValue,
                presentValue,
                discountValue,
                type.getSpreadMonthly(),
                termMonths,
                effectiveBase,
                exchangeRateApplied,
                presentValueInSettlement,
                currency,
                settlementCurrency,
                type.getName());
    }

    /** Prazo em meses: (dueDate - asOfDate) / 30, com escala 6. */
    public BigDecimal termMonths(LocalDate asOfDate, LocalDate dueDate) {
        if (!dueDate.isAfter(asOfDate)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "A data de vencimento deve ser futura.");
        }
        long days = ChronoUnit.DAYS.between(asOfDate, dueDate);
        return BigDecimal.valueOf(days).divide(DAYS_PER_MONTH, 6, RoundingMode.HALF_EVEN);
    }

    /** (1 + taxa)^prazo — potência inteira via BigDecimal, fracionária via Math.pow. */
    private BigDecimal power(BigDecimal base, BigDecimal exponent) {
        if (exponent.stripTrailingZeros().scale() <= 0) {
            return base.pow(exponent.intValueExact());
        }
        return BigDecimal.valueOf(Math.pow(base.doubleValue(), exponent.doubleValue()));
    }
}
