package com.example.Voucher.controller;

import com.example.Voucher.dto.UserCreateRequestDto;
import com.example.Voucher.dto.UserResponseDto;
import com.example.Voucher.entity.User;
import com.example.Voucher.service.UserService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @PostMapping
    @PreAuthorize("hasAuthority(@roleProperties.getAdmin())")
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody UserCreateRequestDto requestDto) {

        if (userService.existsByEmail(requestDto.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        if (userService.existsByPhoneNumber(requestDto.getPhoneNumber())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User user = new User(
                requestDto.getFirstName(),
                requestDto.getLastName(),
                requestDto.getPassword(),
                requestDto.getPhoneNumber(),
                requestDto.getEmail(),
                LocalDateTime.now()
        );

        User savedUser = userService.createUser(user);
        return new ResponseEntity<>(
                UserResponseDto.fromEntity(savedUser),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority(@roleProperties.getAdmin())")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers()
                .stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}
