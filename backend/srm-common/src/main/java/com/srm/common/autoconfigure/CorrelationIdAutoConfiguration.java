package com.srm.common.autoconfigure;

import com.srm.common.correlation.CorrelationIdClientHttpRequestInterceptor;
import com.srm.common.correlation.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuração do correlation id para aplicações Servlet: registra o filtro HTTP e o
 * interceptor de propagação para chamadas entre serviços.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class CorrelationIdAutoConfiguration {

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    public CorrelationIdClientHttpRequestInterceptor correlationIdClientHttpRequestInterceptor() {
        return new CorrelationIdClientHttpRequestInterceptor();
    }
}
