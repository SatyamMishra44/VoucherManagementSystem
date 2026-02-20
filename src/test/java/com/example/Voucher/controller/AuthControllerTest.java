package com.example.Voucher.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.Voucher.dto.AuthLoginRequestDto;
import com.example.Voucher.dto.AuthRegisterRequestDto;
import com.example.Voucher.dto.AuthResponseDto;
import com.example.Voucher.dto.LogoutRequestDto;
import com.example.Voucher.dto.RefreshTokenRequestDto;
import com.example.Voucher.exception.GlobalExceptionHandler;
import com.example.Voucher.security.JwtAuthenticationFilter;
import com.example.Voucher.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void register_whenValidRequest_returnsCreated() throws Exception {
        AuthRegisterRequestDto request = new AuthRegisterRequestDto();
        request.setFirstName("Sam");
        request.setLastName("K");
        request.setEmail("sam@example.com");
        request.setPhoneNumber("9876543210");
        request.setPassword("secret123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(authService).register(any(AuthRegisterRequestDto.class));
    }

    @Test
    void register_whenValidationFails_returnsBadRequest() throws Exception {
        AuthRegisterRequestDto request = new AuthRegisterRequestDto();
        request.setFirstName("Sam");
        request.setLastName("K");
        request.setEmail("bad email");
        request.setPhoneNumber("123");
        request.setPassword("short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenValidRequest_returnsAuthResponse() throws Exception {
        AuthLoginRequestDto request = new AuthLoginRequestDto();
        request.setEmail("sam@example.com");
        request.setPassword("secret123");

        when(authService.login(any(AuthLoginRequestDto.class)))
                .thenReturn(new AuthResponseDto("access-token", "refresh-token", 300, 1200));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void refresh_whenTokenMissing_returnsBadRequest() throws Exception {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void logout_whenValidRequest_returnsNoContent() throws Exception {
        LogoutRequestDto request = new LogoutRequestDto();
        request.setRefreshToken("refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).logout(any(LogoutRequestDto.class));
    }
}
