package com.srm.credit.service;

import com.srm.common.error.BusinessException;
import com.srm.credit.domain.Receivable;
import com.srm.credit.domain.ReceivableRepository;
import com.srm.credit.domain.ReceivableStatus;
import com.srm.credit.domain.ReceivableType;
import com.srm.credit.domain.ReceivableTypeRepository;
import com.srm.credit.dto.CreateReceivableRequest;
import com.srm.credit.dto.CreateReceivablesBatchRequest;
import com.srm.credit.dto.CreateReceivablesBatchResponse;
import com.srm.credit.dto.PageResponse;
import com.srm.credit.dto.ReceivableResponse;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro e consulta de recebíveis. */
@Service
public class ReceivableService {

    private static final Logger log = LoggerFactory.getLogger(ReceivableService.class);

    private final ReceivableRepository receivableRepository;
    private final ReceivableTypeRepository receivableTypeRepository;

    public ReceivableService(
            ReceivableRepository receivableRepository,
            ReceivableTypeRepository receivableTypeRepository) {
        this.receivableRepository = receivableRepository;
        this.receivableTypeRepository = receivableTypeRepository;
    }

    @Transactional
    public CreateReceivablesBatchResponse createBatch(CreateReceivablesBatchRequest request) {
        List<ReceivableResponse> created =
                request.receivables().stream()
                        .map(this::createSingle)
                        .map(ReceivableResponse::from)
                        .toList();
        log.info("Lote criado com {} recebíveis", created.size());
        return new CreateReceivablesBatchResponse(created.size(), created);
    }

    @Transactional
    public Receivable createSingle(CreateReceivableRequest item) {
        ReceivableType type =
                receivableTypeRepository
                        .findById(item.receivableTypeId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                HttpStatus.BAD_REQUEST,
                                                "Tipo de recebível não encontrado: "
                                                        + item.receivableTypeId()));
        Receivable receivable =
                new Receivable(
                        item.cedenteId(), type, item.faceValue(), item.dueDate(), item.currency());
        return receivableRepository.save(receivable);
    }

    public ReceivableResponse findById(UUID id) {
        Receivable receivable =
                receivableRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Recebível não encontrado: " + id));
        return ReceivableResponse.from(receivable);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReceivableResponse> list(
            String status, String currency, UUID cedenteId, Pageable pageable) {
        Specification<Receivable> spec = buildFilter(status, currency, cedenteId);
        Page<Receivable> page = receivableRepository.findAll(spec, pageable);
        return PageResponse.of(
                page.getContent().stream().map(ReceivableResponse::from).toList(),
                pageable,
                page.getTotalElements());
    }

    private Specification<Receivable> buildFilter(String status, String currency, UUID cedenteId) {
        ReceivableStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = ReceivableStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Status inválido: " + status);
            }
        }
        final ReceivableStatus parsedStatus = statusFilter;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (parsedStatus != null) {
                predicates.add(cb.equal(root.get("status"), parsedStatus));
            }
            if (currency != null && !currency.isBlank()) {
                predicates.add(cb.equal(root.get("currency"), currency));
            }
            if (cedenteId != null) {
                predicates.add(cb.equal(root.get("cedenteId"), cedenteId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
