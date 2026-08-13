package com.srm.currency.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.srm.common.error.BusinessException;
import com.srm.currency.domain.Currency;
import com.srm.currency.domain.CurrencyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CurrencyServiceTest {

    private final CurrencyRepository repository = mock(CurrencyRepository.class);
    private final CurrencyService service = new CurrencyService(repository);

    @Test
    void listsOnlyActiveCurrencies() {
        when(repository.findByActiveTrueOrderByCode())
                .thenReturn(List.of(new Currency("BRL", "Real", "R$")));

        assertThat(service.listActive()).hasSize(1).extracting("code").containsExactly("BRL");
    }

    @Test
    void requireCurrencyAcceptsKnownCode() {
        when(repository.findByCode("USD"))
                .thenReturn(Optional.of(new Currency("USD", "Dólar", "$")));

        assertThat(service.requireCurrency("USD").getCode()).isEqualTo("USD");
    }

    @Test
    void requireCurrencyRejectsUnknownCode() {
        when(repository.findByCode("EUR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireCurrency("EUR"))
                .isInstanceOf(BusinessException.class);
    }
}
