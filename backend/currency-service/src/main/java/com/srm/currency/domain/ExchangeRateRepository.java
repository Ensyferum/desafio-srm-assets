package com.srm.currency.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    Optional<ExchangeRate> findByFromCurrencyAndToCurrencyAndEffectiveDate(
            String fromCurrency, String toCurrency, LocalDate effectiveDate);

    List<ExchangeRate> findByFromCurrencyAndToCurrencyOrderByEffectiveDateDesc(
            String fromCurrency, String toCurrency);

    List<ExchangeRate>
            findByFromCurrencyAndToCurrencyAndEffectiveDateBetweenOrderByEffectiveDateDesc(
                    String fromCurrency, String toCurrency, LocalDate start, LocalDate end);

    boolean existsByFromCurrencyAndToCurrencyAndEffectiveDate(
            String fromCurrency, String toCurrency, LocalDate effectiveDate);
}
