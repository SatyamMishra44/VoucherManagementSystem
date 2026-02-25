package com.example.Voucher.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.Voucher.dto.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleIllegalArgument_returns400() {
        ResponseEntity<ApiError> response =
                handler.handleIllegalArgument(new IllegalArgumentException("Invalid input"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid input", response.getBody().getMessage());
    }

    @Test
    void handleInvalidRefreshToken_returns401() {
        ResponseEntity<ApiError> response =
                handler.handleInvalidRefreshToken(new InvalidRefreshTokenException("Invalid refresh token"));

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Invalid refresh token", response.getBody().getMessage());
    }

    @Test
    void handleAuthentication_returns401() {
        ResponseEntity<ApiError> response =
                handler.handleAuthentication(new BadCredentialsException("Bad credentials"));

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Invalid email or password", response.getBody().getMessage());
    }

    @Test
    void handleAccessDenied_returns403() {
        ResponseEntity<ApiError> response =
                handler.handleAccessDenied(new AccessDeniedException("Access denied"));

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Access denied", response.getBody().getMessage());
    }

    @Test
    void handleDataAccess_returns503() {
        ResponseEntity<ApiError> response =
                handler.handleDataAccess(new DataAccessResourceFailureException("Redis down"));

        assertEquals(503, response.getStatusCode().value());
        assertEquals("Database/Redis is unavailable", response.getBody().getMessage());
    }

    @Test
    void handleUnhandled_returns500() {
        ResponseEntity<ApiError> response =
                handler.handleUnhandled(new RuntimeException("Unexpected"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Unexpected server error", response.getBody().getMessage());
    }
}
