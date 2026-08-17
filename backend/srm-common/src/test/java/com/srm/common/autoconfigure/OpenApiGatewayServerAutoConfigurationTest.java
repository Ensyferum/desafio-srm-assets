package com.srm.common.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Cobertura do {@link OpenApiGatewayServerAutoConfiguration} (RF04 — Try it out via gateway). */
class OpenApiGatewayServerAutoConfigurationTest {

    private final OpenApiGatewayServerAutoConfiguration config =
            new OpenApiGatewayServerAutoConfiguration();

    @Test
    void publicGatewayServerCustomizerForcesRelativeGatewayServer() {
        OpenAPI openApi = new OpenAPI();
        openApi.servers(List.of(new Server().url("http://credit-service:8080")));

        config.publicGatewayServerCustomizer().customise(openApi);

        assertThat(openApi.getServers()).hasSize(1);
        Server server = openApi.getServers().get(0);
        assertThat(server.getUrl()).isEqualTo("/");
        assertThat(server.getDescription()).contains("Gateway público");
    }
}
