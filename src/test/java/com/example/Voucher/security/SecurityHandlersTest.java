package com.example.Voucher.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

class SecurityHandlersTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void authenticationEntryPoint_returns401WithApiErrorBody() throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new AuthenticationException("auth required") {
        });

        assertEquals(401, response.getStatus());
        String body = response.getContentAsString();
        assertEquals("Authentication required", objectMapper.readTree(body).get("message").asText());
        assertEquals("Unauthorized", objectMapper.readTree(body).get("error").asText());
    }

    @Test
    void accessDeniedHandler_returns403WithApiErrorBody() throws Exception {
        RestAccessDeniedHandler deniedHandler = new RestAccessDeniedHandler(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        deniedHandler.handle(request, response, new AccessDeniedException("forbidden"));

        assertEquals(403, response.getStatus());
        String body = response.getContentAsString();
        assertEquals("Access denied", objectMapper.readTree(body).get("message").asText());
        assertTrue(objectMapper.readTree(body).get("timestamp") != null);
    }
}
