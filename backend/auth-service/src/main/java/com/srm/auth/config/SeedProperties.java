package com.srm.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Usuários padrão criados no startup (propriedades {@code app.seed.*}). */
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(SeedUser admin, SeedUser manager, SeedUser operator) {

    public record SeedUser(String username, String password) {}
}
