package com.srm.common.autoconfigure;

import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuração do OpenAPI para agregação via gateway (RF04).
 *
 * <p>O springdoc gera o campo {@code servers} do documento OpenAPI a partir do host da requisição
 * (ex.: {@code http://credit-service:8080} — host interno do container), o que quebraria o "Try it
 * out" do Swagger UI quando acessado pela URL pública do gateway. Este customizer força uma URL
 * relativa ({@code /}), que o Swagger UI resolve contra a origin atual da página — ou seja, o
 * próprio gateway.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(OpenApiCustomizer.class)
public class OpenApiGatewayServerAutoConfiguration {

    @Bean
    public OpenApiCustomizer publicGatewayServerCustomizer() {
        return openApi ->
                openApi.servers(
                        List.of(
                                new Server()
                                        .url("/")
                                        .description("Gateway público (SRM Credit Engine)")));
    }
}
