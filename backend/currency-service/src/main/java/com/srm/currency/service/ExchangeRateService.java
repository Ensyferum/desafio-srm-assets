package com.srm.currency.service;

import com.srm.common.error.BusinessException;
import com.srm.common.event.FxUpdatedEvent;
import com.srm.currency.domain.ExchangeRate;
import com.srm.currency.domain.ExchangeRateRepository;
import com.srm.currency.dto.ExchangeRateRequest;
import com.srm.currency.dto.ExchangeRateResponse;
import com.srm.currency.fx.FxRateCache;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de taxas de câmbio (RF01): upsert por par+data, consulta com cache Redis (TTL 5 min) e
 * fallback para a taxa mais recente vigente.
 */
@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyService currencyService;
    private final FxRateCache cache;
    private final ApplicationEventPublisher eventPublisher;

    public ExchangeRateService(
            ExchangeRateRepository exchangeRateRepository,
            CurrencyService currencyService,
            FxRateCache cache,
            ApplicationEventPublisher eventPublisher) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.currencyService = currencyService;
        this.cache = cache;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ExchangeRateResponse createOrUpdate(ExchangeRateRequest request, String createdBy) {
        validatePair(request.fromCurrency(), request.toCurrency());

        ExchangeRate rate =
                exchangeRateRepository
                        .findByFromCurrencyAndToCurrencyAndEffectiveDate(
                                request.fromCurrency(),
                                request.toCurrency(),
                                request.effectiveDate())
                        .map(
                                existing -> {
                                    existing.setRate(request.rate());
                                    existing.setCreatedBy(createdBy);
                                    return existing;
                                })
                        .orElseGet(
                                () ->
                                        new ExchangeRate(
                                                request.fromCurrency(),
                                                request.toCurrency(),
                                                request.rate(),
                                                request.effectiveDate(),
                                                createdBy));

        ExchangeRate saved = exchangeRateRepository.save(rate);
        cache.evict(request.fromCurrency(), request.toCurrency(), request.effectiveDate());

        // Evento publicado apenas após o commit (@TransactionalEventListener)
        eventPublisher.publishEvent(
                new FxUpdatedEvent(
                        saved.getFromCurrency(),
                        saved.getToCurrency(),
                        saved.getRate(),
                        saved.getEffectiveDate(),
                        createdBy));

        log.info(
                "Taxa {} → {} = {} em {} registrada por {}",
                saved.getFromCurrency(),
                saved.getToCurrency(),
                saved.getRate(),
                saved.getEffectiveDate(),
                createdBy);
        return ExchangeRateResponse.from(saved);
    }

    /**
     * Consulta a taxa do par na data informada. Se não existir exatamente na data, retorna a taxa
     * mais recente anterior (fallback para liquidação).
     */
    public ExchangeRateResponse findRate(String from, String to, LocalDate date) {
        validatePair(from, to);
        LocalDate target = date == null ? LocalDate.now() : date;

        return cache.get(from, to, target)
                .orElseGet(
                        () -> {
                            ExchangeRateResponse response = findFromDatabase(from, to, target);
                            cache.put(from, to, target, response);
                            return response;
                        });
    }

    public List<ExchangeRateResponse> list(
            String from, String to, LocalDate startDate, LocalDate endDate) {
        if (from != null && to != null) {
            if (startDate != null && endDate != null) {
                return exchangeRateRepository
                        .findByFromCurrencyAndToCurrencyAndEffectiveDateBetweenOrderByEffectiveDateDesc(
                                from, to, startDate, endDate)
                        .stream()
                        .map(ExchangeRateResponse::from)
                        .toList();
            }
            return exchangeRateRepository
                    .findByFromCurrencyAndToCurrencyOrderByEffectiveDateDesc(from, to)
                    .stream()
                    .map(ExchangeRateResponse::from)
                    .toList();
        }
        return exchangeRateRepository.findAll().stream().map(ExchangeRateResponse::from).toList();
    }

    private ExchangeRateResponse findFromDatabase(String from, String to, LocalDate date) {
        Optional<ExchangeRate> exact =
                exchangeRateRepository.findByFromCurrencyAndToCurrencyAndEffectiveDate(
                        from, to, date);
        if (exact.isPresent()) {
            return ExchangeRateResponse.from(exact.get());
        }
        return exchangeRateRepository
                .findByFromCurrencyAndToCurrencyOrderByEffectiveDateDesc(from, to)
                .stream()
                .filter(rate -> !rate.getEffectiveDate().isAfter(date))
                .findFirst()
                .map(ExchangeRateResponse::from)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        HttpStatus.NOT_FOUND,
                                        "Taxa de câmbio não encontrada para "
                                                + from
                                                + "→"
                                                + to
                                                + " na data "
                                                + date));
    }

    private void validatePair(String from, String to) {
        if (from.equalsIgnoreCase(to)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "As moedas de origem e destino devem ser diferentes.");
        }
        currencyService.requireCurrency(from);
        currencyService.requireCurrency(to);
    }
}
