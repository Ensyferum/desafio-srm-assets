package com.srm.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Credenciais de login. */
public record LoginRequest(
        @NotBlank(message = "username é obrigatório") String username,
        @NotBlank(message = "password é obrigatório") String password) {}
