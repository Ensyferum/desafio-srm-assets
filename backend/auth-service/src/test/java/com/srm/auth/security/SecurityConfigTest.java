package com.srm.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.auth.domain.Role;
import com.srm.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class SecurityConfigTest {

    private static final String SECRET =
            "c2VjdXJlLWRldi1zZWNyZXQtY2hhbmdlLW1lLWluLXByb2R1Y3Rpb24tMjAyNi1zcm0tc3Jt";

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void passwordEncoderHashesWithBCrypt() {
        PasswordEncoder encoder = config.passwordEncoder();

        String hash = encoder.encode("Senha@123");

        assertThat(hash).startsWith("$2");
        assertThat(encoder.matches("Senha@123", hash)).isTrue();
        assertThat(encoder.matches("outra", hash)).isFalse();
    }

    @Test
    void jwtDecoderDecodesTokensIssuedByJwtService() {
        JwtService jwtService = new JwtService(SECRET, "https://srm-credit-engine", 480);
        String token = jwtService.issueToken(new User("admin", "hash", "Admin", Role.ADMIN));

        JwtDecoder decoder = config.jwtDecoder(SECRET);
        Jwt decoded = decoder.decode(token);

        assertThat(decoded.getSubject()).isEqualTo("admin");
        assertThat(decoded.getClaimAsString("roles")).isEqualTo("ADMIN");
    }

    @Test
    void corsConfigurationExposesCorrelationHeader() {
        CorsConfigurationSource source = config.corsConfigurationSource();

        assertThat(source).isNotNull();
        CorsConfiguration cors =
                ((UrlBasedCorsConfigurationSource) source).getCorsConfigurations().get("/**");
        assertThat(cors).isNotNull();
        assertThat(cors.getExposedHeaders()).contains("X-Correlation-Id");
    }
}
