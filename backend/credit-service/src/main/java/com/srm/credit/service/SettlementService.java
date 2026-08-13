package com.srm.credit.service;

import com.srm.common.error.BusinessException;
import com.srm.common.event.SettlementEvent;
import com.srm.credit.config.SettlementMetrics;
import com.srm.credit.domain.Receivable;
import com.srm.credit.domain.ReceivableRepository;
import com.srm.credit.domain.ReceivableStatus;
import com.srm.credit.domain.Transaction;
import com.srm.credit.domain.TransactionRepository;
import com.srm.credit.dto.SettleRequest;
import com.srm.credit.dto.SettleResponse;
import com.srm.credit.pricing.PricingCalculator;
import com.srm.credit.pricing.PricingResult;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liquidação de recebíveis com ACID (RF03): a transação de liquidação e a mudança de status do
 * recebível ocorrem na mesma transação do banco. O {@code @Version} do recebível garante optimistic
 * locking — liquidações concorrentes geram {@code OptimisticLockingFailureException} → 409.
 */
@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final ReceivableRepository receivableRepository;
    private final TransactionRepository transactionRepository;
    private final PricingCalculator pricingCalculator;
    private final SettlementMetrics metrics;
    private final ApplicationEventPublisher eventPublisher;

    public SettlementService(
            ReceivableRepository receivableRepository,
            TransactionRepository transactionRepository,
            PricingCalculator pricingCalculator,
            SettlementMetrics metrics,
            ApplicationEventPublisher eventPublisher) {
        this.receivableRepository = receivableRepository;
        this.transactionRepository = transactionRepository;
        this.pricingCalculator = pricingCalculator;
        this.metrics = metrics;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SettleResponse settle(UUID receivableId, SettleRequest request, String createdBy) {
        log.info(
                "Iniciando liquidação do recebível {} em {} (criado por {})",
                receivableId,
                request.settlementCurrency(),
                createdBy);

        Receivable receivable =
                receivableRepository
                        .findById(receivableId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Recebível não encontrado: " + receivableId));

        if (receivable.getStatus() != ReceivableStatus.PENDING) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Recebível não pode ser liquidado: status atual é " + receivable.getStatus());
        }

        PricingResult pricing =
                pricingCalculator.calculate(
                        receivable,
                        PricingCalculator.DEFAULT_BASE_RATE,
                        request.settlementCurrency(),
                        LocalDate.now());

        Transaction transaction =
                new Transaction(
                        receivable,
                        pricing.presentValue(),
                        pricing.discountValue(),
                        request.settlementCurrency(),
                        pricing.exchangeRateApplied(),
                        createdBy == null ? "anonymous" : createdBy);

        receivable.markSettled();

        // saveAndFlush força o check de versão agora: conflito vira 409 no handler
        receivableRepository.saveAndFlush(receivable);
        transactionRepository.save(transaction);

        // Publicado apenas após o commit (@TransactionalEventListener)
        eventPublisher.publishEvent(
                new SettlementEvent(
                        transaction.getId(),
                        receivable.getId(),
                        receivable.getCedenteId(),
                        receivable.getFaceValue(),
                        transaction.getPresentValue(),
                        transaction.getDiscountValue(),
                        receivable.getCurrency(),
                        transaction.getSettlementCurrency(),
                        transaction.getExchangeRateApplied(),
                        transaction.getSettledAt(),
                        transaction.getStatus().name()));

        metrics.countSettlement(transaction.getSettlementCurrency());

        log.info(
                "Liquidação concluída: transactionId={}, PV={}, deságio={}, moeda={}",
                transaction.getId(),
                transaction.getPresentValue(),
                transaction.getDiscountValue(),
                transaction.getSettlementCurrency());
        return SettleResponse.from(transaction, pricing);
    }
}
