package com.srm.auth.controller;

import com.srm.auth.dto.CreateUserRequest;
import com.srm.auth.dto.LoginRequest;
import com.srm.auth.dto.LoginResponse;
import com.srm.auth.dto.UserResponse;
import com.srm.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de autenticação e gestão de usuários. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Login, criação de usuários e usuário corrente")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e emite um JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria usuário (requer role ADMIN)")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return authService.createUser(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna o usuário autenticado")
    public UserResponse currentUser(
            @RequestHeader(value = "X-Username", required = false) String username) {
        return authService.currentUser(username);
    }
}
