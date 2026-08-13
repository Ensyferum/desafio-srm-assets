package com.srm.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.auth.dto.LoginResponse;
import com.srm.auth.dto.UserResponse;
import com.srm.auth.service.AuthService;
import com.srm.common.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthService authService;

    @Test
    void loginReturnsJwt() throws Exception {
        when(authService.login(any()))
                .thenReturn(
                        new LoginResponse("jwt-token", "Bearer", 28800, "admin", "Admin", "ADMIN"));

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"admin\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void rejectsLoginWithoutPassword() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"admin\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsUser() throws Exception {
        when(authService.createUser(any()))
                .thenReturn(
                        new UserResponse(
                                UUID.randomUUID(),
                                "newuser",
                                "Novo Usuário",
                                "MANAGER",
                                true,
                                Instant.now()));

        mockMvc.perform(
                        post("/api/v1/auth/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "username": "newuser",
                                          "password": "Senha@123",
                                          "fullName": "Novo Usuário",
                                          "role": "MANAGER"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void currentUserFromHeader() throws Exception {
        when(authService.currentUser("operator1"))
                .thenReturn(
                        new UserResponse(
                                UUID.randomUUID(),
                                "operator1",
                                "Operador",
                                "OPERATOR",
                                true,
                                Instant.now()));

        mockMvc.perform(get("/api/v1/auth/me").header("X-Username", "operator1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("operator1"));
    }
}
