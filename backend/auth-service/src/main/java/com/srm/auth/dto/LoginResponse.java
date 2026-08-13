package com.srm.auth.dto;

/** Resposta do login contendo o JWT. */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String username,
        String fullName,
        String role) {}
