package com.srm.currency.service;

import com.srm.common.error.BusinessException;
import com.srm.currency.domain.Currency;
import com.srm.currency.domain.CurrencyRepository;
import com.srm.currency.dto.CurrencyResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Consulta e validação de moedas. */
@Service
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    public CurrencyService(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    public List<CurrencyResponse> listActive() {
        return currencyRepository.findByActiveTrueOrderByCode().stream()
                .map(CurrencyResponse::from)
                .toList();
    }

    /** Valida que a moeda existe e está ativa; caso contrário lança 400. */
    public Currency requireCurrency(String code) {
        return currencyRepository
                .findByCode(code)
                .filter(Currency::isActive)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        HttpStatus.BAD_REQUEST, "Moeda desconhecida: " + code));
    }
}
