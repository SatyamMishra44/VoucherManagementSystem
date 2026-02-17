package com.example.Voucher.controller;

import com.example.Voucher.entity.User;
import com.example.Voucher.security.JwtAuthenticationFilter;
import com.example.Voucher.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getAllUsers_whenUsersExist_shouldReturnOkWithList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(
                new User("A", "Admin", "pwd", "9999999999", "admin@example.com", LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());
    }
}
