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
import com.example.Voucher.security.CustomUserDetailsService;
import com.example.Voucher.security.RoleProperties;
import com.example.Voucher.security.TenantAwareUserDetails;
import com.example.Voucher.tenant.TenantContext;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final RoleProperties roleProperties;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;

    public AuthService(
            UserService userService,
            RoleRepository roleRepository,
            RoleProperties roleProperties,
            JwtService jwtService,
            JwtProperties jwtProperties,
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            StringRedisTemplate redisTemplate) {
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
        Long tenantId = TenantContext.requireTenantId();
        log.info("action=register started | email={} tenantId={}", request.getEmail(), tenantId);
        if (userService.existsByEmail(request.getEmail())) {
            log.warn("action=register failed | email={} reason=Email already in use", request.getEmail());
            throw new IllegalArgumentException("Email already in use");
        }
        if (userService.existsByPhoneNumber(request.getPhoneNumber())) {
            log.warn("action=register failed | phone={} reason=Phone number already in use", request.getPhoneNumber());
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
                LocalDateTime.now());
        user.setTenantId(tenantId);
        user.addRole(userRole);

        userService.createUser(user);
        log.info("action=register completed | email={} userId={} tenantId={}", request.getEmail(), user.getId(),
                tenantId);
    }

    @Transactional
    public AuthResponseDto login(AuthLoginRequestDto request) {
        log.info("action=login started | email={}", request.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception ex) {
            log.warn("action=login failed | email={} reason={}", request.getEmail(), ex.getMessage());
            throw ex;
        }

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsernameAndTenantId(
                request.getEmail(),
                user.getTenantId());

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken(user.getEmail(), user.getTenantId(), refreshToken);

        log.info("action=login completed | email={} userId={} tenantId={}", request.getEmail(), user.getId(),
                user.getTenantId());
        return new AuthResponseDto(
                accessToken,
                refreshToken,
                jwtProperties.getAccessExpirationSeconds(),
                jwtProperties.getRefreshExpirationSeconds());
    }

    public AuthResponseDto refresh(RefreshTokenRequestDto request) {
        log.info("action=refresh started");
        String rawRefreshToken = request.getRefreshToken();
        String email;
        Long tokenTenantId;
        try {
            email = jwtService.extractUsername(rawRefreshToken);
            tokenTenantId = jwtService.extractTenantId(rawRefreshToken);
        } catch (Exception ex) {
            log.warn("action=refresh failed | reason=Token parsing error: {}", ex.getMessage());
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
        if (tokenTenantId == null) {
            log.warn("action=refresh failed | reason=No tenantId in token");
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
        if (!tokenTenantId.equals(TenantContext.requireTenantId())) {
            log.warn("action=refresh failed | email={} reason=Tenant mismatch tokenTenant={} contextTenant={}",
                    email, tokenTenantId, TenantContext.requireTenantId());
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsernameAndTenantId(email, tokenTenantId);
        if (!(userDetails instanceof TenantAwareUserDetails tenantAwareUserDetails)) {
            log.warn("action=refresh failed | email={} reason=Not TenantAwareUserDetails", email);
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
        if (!tenantAwareUserDetails.isEnabled()) {
            log.warn("action=refresh failed | email={} reason=Account disabled", email);
            throw new InvalidRefreshTokenException("User account is disabled");
        }

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (!user.isEnabled() || !user.getTenantId().equals(tokenTenantId)) {
            log.warn("action=refresh failed | email={} reason=User disabled or tenant mismatch", email);
            throw new InvalidRefreshTokenException("User account is disabled");
        }

        if (!jwtService.isRefreshTokenValid(rawRefreshToken, userDetails)) {
            log.warn("action=refresh failed | email={} reason=JWT validation failed", email);
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        String key = refreshTokenKey(rawRefreshToken, tokenTenantId);
        String storedSubject = redisTemplate.opsForValue().get(key);
        String expectedSubject = tokenTenantId + ":" + email;
        if (storedSubject == null || !storedSubject.equals(expectedSubject)) {
            log.warn("action=refresh failed | email={} reason=Redis token not found or subject mismatch", email);
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateAccessToken(userDetails);

        log.info("action=refresh completed | email={} tenantId={}", email, tokenTenantId);
        return new AuthResponseDto(
                newAccessToken,
                rawRefreshToken,
                jwtProperties.getAccessExpirationSeconds(),
                jwtProperties.getRefreshExpirationSeconds());
    }

    public void logout(LogoutRequestDto request) {
        log.info("action=logout started");
        Long tenantId = jwtService.extractTenantId(request.getRefreshToken());
        if (tenantId != null) {
            redisTemplate.delete(refreshTokenKey(request.getRefreshToken(), tenantId));
            log.info("action=logout completed | tenantId={} tokenRevoked=true", tenantId);
        } else {
            log.warn("action=logout | reason=Could not extract tenantId from refresh token");
        }
    }

    private void saveRefreshToken(String email, Long tenantId, String rawRefreshToken) {
        redisTemplate.opsForValue().set(
                refreshTokenKey(rawRefreshToken, tenantId),
                tenantId + ":" + email,
                Duration.ofSeconds(jwtProperties.getRefreshExpirationSeconds()));
    }

    private String refreshTokenKey(String token, Long tenantId) {
        return "refresh:" + tenantId + ":" + token;
    }
}
