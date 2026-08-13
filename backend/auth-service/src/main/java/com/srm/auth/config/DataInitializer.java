package com.srm.auth.config;

import com.srm.auth.domain.Role;
import com.srm.auth.domain.User;
import com.srm.auth.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Cria usuários padrão (se não existirem) para facilitar o setup local. */
@Component
@EnableConfigurationProperties(SeedProperties.class)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties properties;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SeedProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seed(properties.admin(), "Administrador do Sistema", Role.ADMIN);
        seed(properties.manager(), "Gestor de Operações", Role.MANAGER);
        seed(properties.operator(), "Operador de Mesa", Role.OPERATOR);
    }

    private void seed(SeedProperties.SeedUser seedUser, String fullName, Role role) {
        if (userRepository.existsByUsername(seedUser.username())) {
            return;
        }
        userRepository.save(
                new User(
                        seedUser.username(),
                        passwordEncoder.encode(seedUser.password()),
                        fullName,
                        role));
        log.info("Usuário padrão criado: username={}, role={}", seedUser.username(), role);
    }
}
