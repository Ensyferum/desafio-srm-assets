package com.srm.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

class SecurityConfigTest {

    private static final String SECRET =
            "c2VjdXJlLWRldi1zZWNyZXQtY2hhbmdlLW1lLWluLXByb2R1Y3Rpb24tMjAyNi1zcm0tc3Jt";

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void jwtDecoderDecodesTokensSignedWithSharedSecret() {
        String token = new TestTokenIssuer().issue("manager1", "MANAGER");

        ReactiveJwtDecoder decoder = config.jwtDecoder(SECRET);
        Jwt decoded = decoder.decode(token).block();

        assertThat(decoded).isNotNull();
        assertThat(decoded.getSubject()).isEqualTo("manager1");
        assertThat(decoded.getClaimAsString("roles")).isEqualTo("MANAGER");
    }

    @Test
    void buildsSecurityFilterChainWithJwtAuthz() {
        SecurityWebFilterChain chain =
                config.securityWebFilterChain(
                        ServerHttpSecurity.http(),
                        new JwtRolesConverter(),
                        config.jwtDecoder(SECRET));

        assertThat(chain).isNotNull();
    }

    /** Emite tokens HS256 apenas para os testes. */
    static class TestTokenIssuer {
        private final NimbusJwtEncoder encoder;

        TestTokenIssuer() {
            byte[] keyBytes = Base64.getDecoder().decode(SECRET);
            this.encoder =
                    new NimbusJwtEncoder(
                            new ImmutableSecret<SecurityContext>(
                                    new SecretKeySpec(keyBytes, "HmacSHA256")));
        }

        String issue(String username, String roles) {
            Instant now = Instant.now();
            JwtClaimsSet claims =
                    JwtClaimsSet.builder()
                            .issuer("https://srm-credit-engine")
                            .issuedAt(now)
                            .expiresAt(now.plusSeconds(3600))
                            .subject(username)
                            .claim("roles", roles)
                            .build();
            return encoder.encode(
                            JwtEncoderParameters.from(
                                    JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                    .getTokenValue();
        }
    }
}
