package com.srm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Criação de usuário (apenas ADMIN). */
public record CreateUserRequest(
        @NotBlank(message = "username é obrigatório")
                @Size(max = 50, message = "username deve ter no máximo 50 caracteres")
                String username,
        @NotBlank(message = "password é obrigatória")
                @Size(min = 8, message = "password deve ter no mínimo 8 caracteres")
                String password,
        @NotBlank(message = "fullName é obrigatório")
                @Size(max = 120, message = "fullName deve ter no máximo 120 caracteres")
                String fullName,
        @NotBlank(message = "role é obrigatória")
                @Pattern(
                        regexp = "OPERATOR|MANAGER|ADMIN",
                        message = "role deve ser OPERATOR, MANAGER ou ADMIN")
                String role) {}
