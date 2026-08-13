package com.srm.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.auth.domain.Role;
import com.srm.auth.domain.User;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class JwtServiceTest {

    private static final String SECRET =
            "c2VjdXJlLWRldi1zZWNyZXQtY2hhbmdlLW1lLWluLXByb2R1Y3Rpb24tMjAyNi1zcm0tc3Jt";

    @Test
    void issuesTokenWithExpectedClaims() {
        JwtService jwtService = new JwtService(SECRET, "srm-credit-engine", 480);
        User user = new User("admin", "hash", "Admin", Role.ADMIN);

        String token = jwtService.issueToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.expirationSeconds()).isEqualTo(28800L);

        Jwt decoded = decoder().decode(token);
        assertThat(decoded.getSubject()).isEqualTo("admin");
        assertThat(decoded.getClaimAsString("roles")).isEqualTo("ADMIN");
        assertThat(decoded.getClaimAsString("fullName")).isEqualTo("Admin");
        assertThat(decoded.getIssuer()).isEqualTo("srm-credit-engine");
        assertThat(decoded.getExpiresAt()).isAfter(decoded.getIssuedAt());
    }

    private JwtDecoder decoder() {
        byte[] keyBytes = Base64.getDecoder().decode(SECRET);
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(keyBytes, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
