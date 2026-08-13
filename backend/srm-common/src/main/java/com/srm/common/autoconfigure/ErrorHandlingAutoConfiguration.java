package com.srm.common.autoconfigure;

import com.srm.common.error.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;

/** Registra o {@link GlobalExceptionHandler} em todos os serviços web. */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class ErrorHandlingAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
