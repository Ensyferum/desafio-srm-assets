package com.srm.credit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.srm.common.error.BusinessException;
import com.srm.credit.domain.Receivable;
import com.srm.credit.domain.ReceivableRepository;
import com.srm.credit.domain.ReceivableType;
import com.srm.credit.domain.ReceivableTypeRepository;
import com.srm.credit.dto.CreateReceivableRequest;
import com.srm.credit.dto.CreateReceivablesBatchRequest;
import com.srm.credit.dto.CreateReceivablesBatchResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}
