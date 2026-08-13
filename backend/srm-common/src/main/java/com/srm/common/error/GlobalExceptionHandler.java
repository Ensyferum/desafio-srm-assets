package com.srm.common.error;

import com.srm.common.correlation.CorrelationIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Tratamento global e resiliente de exceções (RNF01):
 *
 * <ul>
 *   <li>erros de negócio → 4xx com mensagens claras;
 *   <li>conflito de concorrência (optimistic locking) → 409;
 *   <li>erros inesperados → 500 com {@code errorId} para rastreio em logs.
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request, null, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(
                        fieldError ->
                                fieldErrors.put(
                                        fieldError.getField(), fieldError.getDefaultMessage()));
        return build(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida — verifique os campos apontados.",
                request,
                fieldErrors,
                null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(
            Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Requisição inválida.", request, null, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        return build(
                HttpStatus.CONFLICT,
                "Conflito de concorrência: o registro foi modificado por outro usuário. Tente novamente.",
                request,
                null,
                null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Recurso não encontrado.", request, null, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        log.error(
                "Erro não tratado. errorId={}, correlationId={}, path={}",
                errorId,
                CorrelationIds.get(),
                request.getRequestURI(),
                ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor. Use o errorId para rastreio: " + errorId,
                request,
                null,
                errorId);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors,
            String errorId) {
        ErrorResponse body =
                new ErrorResponse(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI(),
                        CorrelationIds.get(),
                        errorId,
                        fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
