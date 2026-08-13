package com.srm.gateway.security;

import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Segurança do gateway (RNF03): valida o JWT HS256 emitido pelo auth-service e aplica autorização
 * por role em cada rota.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http, JwtRolesConverter jwtRolesConverter) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(
                        exchanges ->
                                exchanges
                                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/login")
                                        .permitAll()
                                        .pathMatchers("/actuator/health", "/actuator/health/**")
                                        .permitAll()
                                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/users/**")
                                        .hasRole("ADMIN")
                                        .pathMatchers(HttpMethod.POST, "/api/v1/exchange-rates/**")
                                        .hasAnyRole("MANAGER", "ADMIN")
                                        .pathMatchers(HttpMethod.PUT, "/api/v1/exchange-rates/**")
                                        .hasAnyRole("MANAGER", "ADMIN")
                                        .pathMatchers(HttpMethod.PATCH, "/api/v1/exchange-rates/**")
                                        .hasAnyRole("MANAGER", "ADMIN")
                                        .pathMatchers(
                                                HttpMethod.DELETE, "/api/v1/exchange-rates/**")
                                        .hasAnyRole("MANAGER", "ADMIN")
                                        .pathMatchers(HttpMethod.POST, "/api/v1/receivables/**")
                                        .hasAnyRole("OPERATOR", "MANAGER", "ADMIN")
                                        .pathMatchers("/api/v1/**")
                                        .authenticated()
                                        .anyExchange()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(
                                        jwt -> jwt.jwtAuthenticationConverter(jwtRolesConverter)));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(keyBytes, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
