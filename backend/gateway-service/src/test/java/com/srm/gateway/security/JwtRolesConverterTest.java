package com.srm.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRolesConverterTest {

    private final JwtRolesConverter converter = new JwtRolesConverter();

    private Jwt jwtWithRoles(Object roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject("operator1")
                .claims(claims -> claims.put("roles", roles))
                .build();
    }

    @Test
    void mapsSingleRoleClaimToAuthority() {
        AbstractAuthenticationToken token = converter.convert(jwtWithRoles("MANAGER")).block();

        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).extracting("authority").containsExactly("ROLE_MANAGER");
        assertThat(token.getName()).isEqualTo("operator1");
    }

    @Test
    void mapsListRoleClaimToAuthorities() {
        AbstractAuthenticationToken token =
                converter.convert(jwtWithRoles(List.of("OPERATOR", "ADMIN"))).block();

        assertThat(token.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_OPERATOR", "ROLE_ADMIN");
    }

    @Test
    void mapsMissingRoleToNoAuthorities() {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "HS256")
                        .subject("user")
                        .claims(claims -> claims.putAll(Map.of()))
                        .build();

        AbstractAuthenticationToken token = converter.convert(jwt).block();

        assertThat(token.getAuthorities()).isEmpty();
        assertThat(token.getName()).isEqualTo("user");
    }
}
