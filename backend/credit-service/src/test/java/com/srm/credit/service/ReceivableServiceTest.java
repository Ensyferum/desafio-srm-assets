package com.srm.credit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

class ReceivableServiceTest {

    private ReceivableRepository receivableRepository;
    private ReceivableTypeRepository receivableTypeRepository;
    private ReceivableService service;

    private final UUID typeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        receivableRepository = mock(ReceivableRepository.class);
        receivableTypeRepository = mock(ReceivableTypeRepository.class);
        service = new ReceivableService(receivableRepository, receivableTypeRepository);
        when(receivableTypeRepository.findById(typeId))
                .thenReturn(
                        Optional.of(
                                new ReceivableType(
                                        "Duplicata Mercantil", new BigDecimal("0.015"), "Título")));
    }

    @Test
    void createsBatchOfReceivables() {
        when(receivableRepository.save(any(Receivable.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateReceivablesBatchResponse response =
                service.createBatch(
                        new CreateReceivablesBatchRequest(
                                List.of(
                                        new CreateReceivableRequest(
                                                UUID.randomUUID(),
                                                typeId,
                                                new BigDecimal("10000.00"),
                                                LocalDate.now().plusDays(30),
                                                "BRL"),
                                        new CreateReceivableRequest(
                                                UUID.randomUUID(),
                                                typeId,
                                                new BigDecimal("20000.00"),
                                                LocalDate.now().plusDays(60),
                                                "USD"))));

        assertThat(response.created()).isEqualTo(2);
        assertThat(response.receivables()).hasSize(2);
        assertThat(response.receivables().get(0).status()).isEqualTo("PENDING");
    }

    @Test
    void rejectsBatchWithUnknownType() {
        when(receivableTypeRepository.findById(typeId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.createBatch(
                                        new CreateReceivablesBatchRequest(
                                                List.of(
                                                        new CreateReceivableRequest(
                                                                UUID.randomUUID(),
                                                                typeId,
                                                                new BigDecimal("100.00"),
                                                                LocalDate.now().plusDays(30),
                                                                "BRL")))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void findByIdReturnsReceivable() {
        ReceivableType type =
                new ReceivableType("Duplicata Mercantil", new BigDecimal("0.015"), "Título");
        Receivable receivable =
                new Receivable(
                        UUID.randomUUID(),
                        type,
                        new BigDecimal("100.00"),
                        LocalDate.now().plusDays(30),
                        "BRL");
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));

        assertThat(service.findById(receivable.getId()).faceValue()).isEqualByComparingTo("100.00");
    }

    @Test
    void findByIdThrows404WhenMissing() {
        when(receivableRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listsReceivablesWithPagination() {
        ReceivableType type =
                new ReceivableType("Duplicata Mercantil", new BigDecimal("0.015"), "Título");
        Receivable receivable =
                new Receivable(
                        UUID.randomUUID(),
                        type,
                        new BigDecimal("100.00"),
                        LocalDate.now().plusDays(30),
                        "BRL");
        when(receivableRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(receivable), PageRequest.of(0, 20), 1));

        PageResponse<ReceivableResponse> page =
                service.list(null, null, null, PageRequest.of(0, 20));

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.content().get(0).faceValue()).isEqualByComparingTo("100.00");
        assertThat(page.content().get(0).status()).isEqualTo("PENDING");
    }

    @Test
    void listAppliesStatusCurrencyAndCedenteFilters() {
        Root<Receivable> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        when(cb.equal(any(), any())).thenReturn(mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));
        when(receivableRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        UUID cedenteId = UUID.randomUUID();
        service.list("PENDING", "BRL", cedenteId, PageRequest.of(0, 20));

        ArgumentCaptor<Specification<Receivable>> captor =
                ArgumentCaptor.forClass(Specification.class);
        verify(receivableRepository).findAll(captor.capture(), any(Pageable.class));
        captor.getValue().toPredicate(root, query, cb);

        verify(cb).equal(root.get("status"), ReceivableStatus.PENDING);
        verify(cb).equal(root.get("currency"), "BRL");
        verify(cb).equal(root.get("cedenteId"), cedenteId);
    }

    @Test
    void listRejectsInvalidStatus() {
        assertThatThrownBy(() -> service.list("INVALIDO", null, null, PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
