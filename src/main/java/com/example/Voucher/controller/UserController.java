package com.example.Voucher.controller;

import com.example.Voucher.dto.UserResponseDto;
import com.example.Voucher.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @PreAuthorize("hasAnyAuthority(@roleProperties.getPlatformAdmin(), @roleProperties.getTenantAdmin())")
    @Operation(
            summary = "Admin: List All Users",
            description = "Admin-only endpoint to view users. Supports optional filters: search, email, phoneNumber, enabled."
    )
    public ResponseEntity<List<UserResponseDto>> getAllUsers(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) Boolean enabled
    ) {
        List<UserResponseDto> users = userService.getUsersWithFilters(firstName,lastName, email, phoneNumber, enabled)
                .stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}
