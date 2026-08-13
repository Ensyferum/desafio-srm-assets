package com.srm.auth.service;

import com.srm.auth.domain.Role;
import com.srm.auth.domain.User;
import com.srm.auth.domain.UserRepository;
import com.srm.auth.dto.CreateUserRequest;
import com.srm.auth.dto.LoginRequest;
import com.srm.auth.dto.LoginResponse;
import com.srm.auth.dto.UserResponse;
import com.srm.auth.security.JwtService;
import com.srm.common.error.BusinessException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lógica de autenticação e gestão de usuários. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        User user =
                userRepository
                        .findByUsername(request.username())
                        .filter(User::isActive)
                        .orElseThrow(() -> invalidCredentials());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw invalidCredentials();
        }

        String token = jwtService.issueToken(user);
        return new LoginResponse(
                token,
                "Bearer",
                jwtService.expirationSeconds(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name());
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "Já existe um usuário com este username.");
        }
        Role role = Role.valueOf(request.role().toUpperCase(Locale.ROOT));
        User user =
                new User(
                        request.username(),
                        passwordEncoder.encode(request.password()),
                        request.fullName(),
                        role);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse currentUser(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Usuário não identificado.");
        }
        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        return UserResponse.from(user);
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
    }
}
