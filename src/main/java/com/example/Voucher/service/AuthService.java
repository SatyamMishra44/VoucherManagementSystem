package com.example.Voucher.service;

import com.example.Voucher.dto.AuthLoginRequestDto;
import com.example.Voucher.dto.AuthRegisterRequestDto;
import com.example.Voucher.dto.AuthResponseDto;
import com.example.Voucher.dto.LogoutRequestDto;
import com.example.Voucher.dto.RefreshTokenRequestDto;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.User;
import com.example.Voucher.exception.InvalidRefreshTokenException;
import com.example.Voucher.repository.RoleRepository;
import com.example.Voucher.security.JwtProperties;
import com.example.Voucher.security.JwtService;
import com.example.Voucher.security.RoleProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final RoleProperties roleProperties;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;

    public AuthService(
            UserService userService,
            RoleRepository roleRepository,
            RoleProperties roleProperties,
            JwtService jwtService,
            JwtProperties jwtProperties,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            StringRedisTemplate redisTemplate
    ) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.roleProperties = roleProperties;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.redisTemplate = redisTemplate;
    }

    public void register(AuthRegisterRequestDto request) {
        if (userService.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (userService.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already in use");
        }

        Role userRole = roleRepository.findByName(roleProperties.getUser())
                .orElseThrow(() -> new IllegalStateException("Default role not configured"));

        User user = new User(
                request.getFirstName(),
                request.getLastName(),
                request.getPassword(),
                request.getPhoneNumber(),
                request.getEmail(),
                LocalDateTime.now()
        );
        user.addRole(userRole);

        userService.createUser(user);
    }

    @Transactional
    public AuthResponseDto login(AuthLoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken(user.getEmail(), refreshToken);

        return new AuthResponseDto(
                accessToken,
                refreshToken,
                jwtProperties.getAccessExpirationSeconds(),
                jwtProperties.getRefreshExpirationSeconds()
        );
    }

    public AuthResponseDto refresh(RefreshTokenRequestDto request) {
        String rawRefreshToken = request.getRefreshToken();
        String email;
        try {
            email = jwtService.extractUsername(rawRefreshToken);
        } catch (Exception ex) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (!user.isEnabled()) {
            throw new InvalidRefreshTokenException("User account is disabled");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!jwtService.isRefreshTokenValid(rawRefreshToken, userDetails)) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        String key = refreshTokenKey(rawRefreshToken);
        String storedEmail = redisTemplate.opsForValue().get(key);
        if (storedEmail == null || !storedEmail.equals(email)) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateAccessToken(userDetails);

        return new AuthResponseDto(
                newAccessToken,
                rawRefreshToken,
                jwtProperties.getAccessExpirationSeconds(),
                jwtProperties.getRefreshExpirationSeconds()
        );
    }

    public void logout(LogoutRequestDto request) {
        redisTemplate.delete(refreshTokenKey(request.getRefreshToken()));
    }

    private void saveRefreshToken(String email, String rawRefreshToken) {
        redisTemplate.opsForValue().set(
                refreshTokenKey(rawRefreshToken),
                email,
                Duration.ofSeconds(jwtProperties.getRefreshExpirationSeconds())
        );
    }

    private String refreshTokenKey(String token) {
        return "refresh:" + token;
    }
}
