package com.srm.gateway.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Converte o claim {@code roles} do JWT (emitido pelo auth-service) em autoridades {@code ROLE_*}
 * para autorização por role no gateway.
 */
@Component
public class JwtRolesConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        Object roles = jwt.getClaim("roles");
        if (roles instanceof String singleRole) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + singleRole));
        } else if (roles instanceof Collection<?> roleList) {
            roleList.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }
        return Mono.just(
                new JwtAuthenticationToken(jwt, List.copyOf(authorities), jwt.getSubject()));
    }
}
