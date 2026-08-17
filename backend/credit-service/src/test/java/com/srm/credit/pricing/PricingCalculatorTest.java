package com.srm.credit.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.common.error.BusinessException;
import com.srm.credit.domain.Receivable;
import com.srm.credit.domain.ReceivableType;
import com.srm.credit.fx.FxConversionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PricingCalculatorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 8, 12);

    private FxConversionService fxConversionService;
    private PricingCalculator calculator;

    @BeforeEach
    void setUp() {
        fxConversionService = mock(FxConversionService.class);
        PricingStrategyRegistry registry =
                new PricingStrategyRegistry(
                        List.of(new StandardPricingStrategy(), new ChequePreDatadoStrategy()));
        calculator = new PricingCalculator(registry, fxConversionService);
    }

    private ReceivableType duplicata() {
        return new ReceivableType(
                "Duplicata Mercantil", new BigDecimal("0.015"), "Título comercial");
    }

    /**
     * Exemplo da spec: R$ 100.000, 90 dias, spread 1,5% a.m., taxa base 0,5%.
     *
     * <p>Nota: o exemplo da SRM spec afirma PV = 94.232,28, mas o valor matematicamente correto de
     * 100.000 / 1,061208 é 94.232,23 (a spec contém um erro de arredondamento). A implementação
     * segue a fórmula exata.
     */
    @Test
    void matchesSpecExample() {
        PricingResult result =
                calculator.calculate(
                        duplicata(),
                        new BigDecimal("100000.00"),
                        AS_OF.plusDays(90),
                        "BRL",
                        "BRL",
                        new BigDecimal("0.005"),
                        AS_OF);

        assertThat(result.termMonths()).isEqualByComparingTo("3.000000");
        assertThat(result.presentValue()).isEqualByComparingTo("94232.23");
        assertThat(result.discountValue()).isEqualByComparingTo("5767.77");
        assertThat(result.spreadApplied()).isEqualByComparingTo("0.015");
        assertThat(result.exchangeRateApplied()).isNull();
        assertThat(result.presentValueInSettlementCurrency()).isEqualByComparingTo("94232.23");
    }

    /** Cross-currency: título em BRL, liquidação em USD com taxa 5,4523. */
    @Test
    void convertsToSettlementCurrency() {
        when(fxConversionService.getRate("USD", "BRL", AS_OF)).thenReturn(new BigDecimal("5.4523"));

        PricingResult result =
                calculator.calculate(
                        duplicata(),
                        new BigDecimal("100000.00"),
                        AS_OF.plusDays(90),
                        "BRL",
                        "USD",
                        new BigDecimal("0.005"),
                        AS_OF);

        assertThat(result.exchangeRateApplied()).isEqualByComparingTo("5.4523");
        assertThat(result.presentValueInSettlementCurrency()).isEqualByComparingTo("17283.02");
    }

    @Test
    void doesNotCallFxForSameCurrency() {
        PricingResult result =
                calculator.calculate(
                        duplicata(),
                        new BigDecimal("1000.00"),
                        AS_OF.plusDays(30),
                        "USD",
                        "USD",
                        new BigDecimal("0.005"),
                        AS_OF);

        verify(fxConversionService, never()).getRate(any(), any(), any());
        assertThat(result.presentValueInSettlementCurrency())
                .isEqualByComparingTo(result.presentValue());
    }

    @Test
    void rejectsDueDateInThePast() {
        assertThatThrownBy(
                        () ->
                                calculator.calculate(
                                        duplicata(),
                                        new BigDecimal("1000.00"),
                                        AS_OF.minusDays(1),
                                        "BRL",
                                        "BRL",
                                        new BigDecimal("0.005"),
                                        AS_OF))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("futura");
    }

    /** Cheque pré-datado com prazo > 6 meses recebe prêmio extra de risco (0,5 p.p.). */
    @Test
    void chequePreDatadoLongTermAppliesExtraRiskPremium() {
        ReceivableType cheque =
                new ReceivableType("Cheque Pré-datado", new BigDecimal("0.025"), "Cheque");
        // prazo de 7 meses
        PricingResult result =
                calculator.calculate(
                        cheque,
                        new BigDecimal("10000.00"),
                        AS_OF.plusDays(210),
                        "BRL",
                        "BRL",
                        new BigDecimal("0.005"),
                        AS_OF);

        // taxa efetiva = 0,005 + 0,025 + 0,005 (prêmio) = 0,035
        BigDecimal factor = BigDecimal.ONE.add(new BigDecimal("0.035"));
        BigDecimal expected =
                new BigDecimal("10000").divide(factor.pow(7), 2, java.math.RoundingMode.HALF_EVEN);
        assertThat(result.presentValue()).isEqualByComparingTo(expected);
    }

    @Test
    void standardStrategyAppliesBasePlusSpread() {
        ReceivableType nota =
                new ReceivableType("Nota Promissória", new BigDecimal("0.018"), "Nota");
        PricingResult result =
                calculator.calculate(
                        nota,
                        new BigDecimal("1000.00"),
                        AS_OF.plusDays(30),
                        "BRL",
                        "BRL",
                        new BigDecimal("0.005"),
                        AS_OF);

        BigDecimal expected =
                new BigDecimal("1000")
                        .divide(
                                BigDecimal.ONE.add(new BigDecimal("0.023")),
                                2,
                                java.math.RoundingMode.HALF_EVEN);
        assertThat(result.presentValue()).isEqualByComparingTo(expected);
    }

    @Test
    void calculateFromReceivableEntityUsesItsFields() {
        ReceivableType type = duplicata();
        Receivable receivable =
                new Receivable(
                        "11222333000181",
                        type,
                        new BigDecimal("50000.00"),
                        AS_OF.plusDays(60),
                        "BRL");

        PricingResult result =
                calculator.calculate(receivable, new BigDecimal("0.005"), "BRL", AS_OF);

        assertThat(result.faceValue()).isEqualByComparingTo("50000.00");
        assertThat(result.termMonths()).isEqualByComparingTo("2.000000");
    }
}
