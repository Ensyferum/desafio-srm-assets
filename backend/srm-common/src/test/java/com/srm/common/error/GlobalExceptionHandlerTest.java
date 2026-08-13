package com.srm.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("GET", "/api/v1/receivables/123");
    }

    @Test
    void businessExceptionReturnsItsHttpStatusAndMessage() {
        BusinessException ex =
                new BusinessException(HttpStatus.NOT_FOUND, "Recebível não encontrado");

        ResponseEntity<ErrorResponse> result = handler.handleBusiness(ex, request());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody().message()).isEqualTo("Recebível não encontrado");
        assertThat(result.getBody().status()).isEqualTo(404);
        assertThat(result.getBody().path()).isEqualTo("/api/v1/receivables/123");
    }

    @Test
    void validationErrorsAreMappedByField() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "payload");
        binding.addError(new FieldError("payload", "faceValue", "não pode ser nulo"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);

        ResponseEntity<ErrorResponse> result = handler.handleValidation(ex, request());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().fieldErrors()).containsEntry("faceValue", "não pode ser nulo");
    }

    @Test
    void optimisticLockingReturnsConflict() {
        OptimisticLockingFailureException ex =
                new OptimisticLockingFailureException("version conflict");

        ResponseEntity<ErrorResponse> result = handler.handleConflict(ex, request());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().message()).contains("Conflito de concorrência");
    }

    @Test
    void genericErrorReturns500WithErrorId() {
        ResponseEntity<ErrorResponse> result =
                handler.handleGeneric(new IllegalStateException("boom"), request());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody().errorId()).isNotBlank();
        assertThat(result.getBody().message()).contains(result.getBody().errorId());
    }
}
