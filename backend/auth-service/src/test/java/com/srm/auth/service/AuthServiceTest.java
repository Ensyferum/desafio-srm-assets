package com.srm.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.auth.domain.Role;
import com.srm.auth.domain.User;
import com.srm.auth.domain.UserRepository;
import com.srm.auth.dto.CreateUserRequest;
import com.srm.auth.dto.LoginRequest;
import com.srm.auth.dto.LoginResponse;
import com.srm.auth.dto.UserResponse;
import com.srm.auth.security.JwtService;
import com.srm.common.error.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    private User user() {
        return new User("operator1", "hash", "Operador Um", Role.OPERATOR);
    }

    @Test
    void loginIssuesTokenForValidCredentials() {
        User user = user();
        when(userRepository.findByUsername("operator1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha@123", "hash")).thenReturn(true);
        when(jwtService.issueToken(user)).thenReturn("jwt-token");
        when(jwtService.expirationSeconds()).thenReturn(28800L);

        LoginResponse response = authService.login(new LoginRequest("operator1", "Senha@123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("operator1");
        assertThat(response.role()).isEqualTo("OPERATOR");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void loginFailsForUnknownUser() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "x")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(jwtService, never()).issueToken(any());
    }

    @Test
    void loginFailsForWrongPassword() {
        User user = user();
        when(userRepository.findByUsername("operator1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("operator1", "wrong")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createUserEncodesPasswordAndSaves() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        User saved = new User("newuser", "encoded", "Novo Usuário", Role.MANAGER);
        when(passwordEncoder.encode("Senha@123")).thenReturn("encoded");
        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        inv -> {
                            User u = inv.getArgument(0);
                            return new User(
                                    u.getUsername(), u.getPassword(), u.getFullName(), u.getRole());
                        });

        UserResponse response =
                authService.createUser(
                        new CreateUserRequest("newuser", "Senha@123", "Novo Usuário", "MANAGER"));

        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.role()).isEqualTo("MANAGER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("dup")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                authService.createUser(
                                        new CreateUserRequest("dup", "Senha@123", "Dup", "ADMIN")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
    }

    @Test
    void currentUserReturnsUserFromRepository() {
        when(userRepository.findByUsername("operator1")).thenReturn(Optional.of(user()));

        UserResponse response = authService.currentUser("operator1");

        assertThat(response.username()).isEqualTo("operator1");
        assertThat(response.id()).isNull();
    }

    @Test
    void currentUserRejectsMissingHeader() {
        assertThatThrownBy(() -> authService.currentUser(null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
