package com.srm.auth.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.auth.config.SeedProperties.SeedUser;
import com.srm.auth.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class DataInitializerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private final SeedProperties properties =
            new SeedProperties(
                    new SeedUser("admin", "Admin@123"),
                    new SeedUser("manager", "Manager@123"),
                    new SeedUser("operator", "Operator@123"));

    @Test
    void createsAllDefaultUsersWhenNoneExist() throws Exception {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        new DataInitializer(userRepository, passwordEncoder, properties).run();

        verify(userRepository, times(3)).save(any());
    }

    @Test
    void skipsUsersThatAlreadyExist() throws Exception {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(userRepository.existsByUsername("manager")).thenReturn(true);
        when(userRepository.existsByUsername("operator")).thenReturn(true);

        new DataInitializer(userRepository, passwordEncoder, properties).run();

        verify(userRepository, never()).save(any());
    }
}
