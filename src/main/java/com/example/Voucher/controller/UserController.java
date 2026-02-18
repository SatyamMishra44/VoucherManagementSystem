package com.example.Voucher.controller;

import com.example.Voucher.dto.UserResponseDto;
import com.example.Voucher.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "2. Users", description = "User management APIs")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority(@roleProperties.getAdmin())")
    @Operation(
            summary = "Admin: List All Users",
            description = "Admin-only endpoint to view all registered users."
    )
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers()
                .stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}
