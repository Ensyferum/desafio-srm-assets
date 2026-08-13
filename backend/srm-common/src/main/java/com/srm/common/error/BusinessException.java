package com.srm.common.error;

import org.springframework.http.HttpStatus;

/**
 * Exceção de negócio com status HTTP explícito — traduzida pelo {@link GlobalExceptionHandler} em
 * uma resposta JSON padronizada.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
