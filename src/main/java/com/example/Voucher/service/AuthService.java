package com.example.Voucher.service;

import com.example.Voucher.dto.AuthLoginRequestDto;
import com.example.Voucher.dto.AuthRegisterRequestDto;
import com.example.Voucher.dto.AuthResponseDto;
import com.example.Voucher.dto.LogoutRequestDto;
import com.example.Voucher.dto.RefreshTokenRequestDto;
import com.example.Voucher.entity.RefreshToken;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.User;
import com.example.Voucher.exception.InvalidRefreshTokenException;
import com.example.Voucher.repository.RefreshTokenRepository;
import com.example.Voucher.repository.RoleRepository;
import com.example.Voucher.security.JwtProperties;
import com.example.Voucher.security.JwtService;
import com.example.Voucher.security.RoleProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(
            UserService userService,
            RoleRepository roleRepository,
            RoleProperties roleProperties,
            JwtService jwtService,
            JwtProperties jwtProperties,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.roleProperties = roleProperties;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.refreshTokenRepository = refreshTokenRepository;
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

        saveRefreshToken(user, refreshToken);

        return new AuthResponseDto(
                accessToken,
                refreshToken,
                jwtProperties.getAccessExpirationSeconds(),
                jwtProperties.getRefreshExpirationSeconds()
        );
    }

    @Transactional
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

        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (storedToken.isRevoked() || storedToken.isExpired()) {
            throw new InvalidRefreshTokenException("Refresh token expired or revoked");
        }

        String newRefreshToken = jwtService.generateRefreshToken(userDetails);
        String newRefreshTokenHash = hashToken(newRefreshToken);

        storedToken.revoke(newRefreshTokenHash);
        refreshTokenRepository.save(storedToken);

        refreshTokenRepository.save(new RefreshToken(
                user,
                newRefreshTokenHash,
                LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpirationSeconds())
        ));

        String newAccessToken = jwtService.generateAccessToken(userDetails);

        return new AuthResponseDto(
                newAccessToken,
                newRefreshToken,
                jwtProperties.getAccessExpirationSeconds(),
                jwtProperties.getRefreshExpirationSeconds()
        );
    }

    @Transactional
    public void logout(LogoutRequestDto request) {
        String tokenHash = hashToken(request.getRefreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.revoke(null);
                refreshTokenRepository.save(token);
            }
        });
    }

    private void saveRefreshToken(User user, String rawRefreshToken) {
        refreshTokenRepository.save(new RefreshToken(
                user,
                hashToken(rawRefreshToken),
                LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpirationSeconds())
        ));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash refresh token", ex);
        }
    }
}
