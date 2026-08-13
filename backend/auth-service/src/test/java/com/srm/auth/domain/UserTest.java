package com.srm.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void exposesEntityState() {
        User user = new User("operator", "hash", "Operador", Role.OPERATOR);

        assertThat(user.getUsername()).isEqualTo("operator");
        assertThat(user.getPassword()).isEqualTo("hash");
        assertThat(user.getFullName()).isEqualTo("Operador");
        assertThat(user.getRole()).isEqualTo(Role.OPERATOR);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getId()).isNull();
        assertThat(user.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void noArgConstructorIsAvailableForJpa() {
        User user = new User();

        assertThat(user.isActive()).isTrue();
        assertThat(user.getId()).isNull();
    }
}
