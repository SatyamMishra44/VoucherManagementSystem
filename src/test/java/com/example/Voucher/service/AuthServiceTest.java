package com.example.Voucher.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Voucher.dto.AuthLoginRequestDto;
import com.example.Voucher.dto.AuthRegisterRequestDto;
import com.example.Voucher.dto.AuthResponseDto;
import com.example.Voucher.dto.LogoutRequestDto;
import com.example.Voucher.dto.RefreshTokenRequestDto;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.User;
import com.example.Voucher.exception.InvalidRefreshTokenException;
import com.example.Voucher.repository.RoleRepository;
import com.example.Voucher.security.CustomUserDetailsService;
import com.example.Voucher.security.JwtProperties;
import com.example.Voucher.security.JwtService;
import com.example.Voucher.security.RoleProperties;
import com.example.Voucher.security.TenantAwareUserDetails;
import com.example.Voucher.tenant.TenantContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RoleProperties roleProperties;
    @Mock
    private JwtService jwtService;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private AuthRegisterRequestDto registerRequest;
    private AuthLoginRequestDto loginRequest;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
        registerRequest = new AuthRegisterRequestDto();
        registerRequest.setFirstName("Sam");
        registerRequest.setLastName("K");
        registerRequest.setEmail("sam@example.com");
        registerRequest.setPhoneNumber("9876543210");
        registerRequest.setPassword("secret123");

        loginRequest = new AuthLoginRequestDto();
        loginRequest.setEmail("sam@example.com");
        loginRequest.setPassword("secret123");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void register_whenEmailAlreadyExists_throwsIllegalArgumentException() {
        when(userService.existsByEmail("sam@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest));

        assertEquals("Email already in use", ex.getMessage());
        verify(userService, never()).createUser(any());
    }

    @Test
    void register_whenPhoneAlreadyExists_throwsIllegalArgumentException() {
        when(userService.existsByEmail("sam@example.com")).thenReturn(false);
        when(userService.existsByPhoneNumber("9876543210")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest));

        assertEquals("Phone number already in use", ex.getMessage());
        verify(userService, never()).createUser(any());
    }

    @Test
    void register_whenRoleMissing_throwsIllegalStateException() {
        when(userService.existsByEmail("sam@example.com")).thenReturn(false);
        when(userService.existsByPhoneNumber("9876543210")).thenReturn(false);
        when(roleProperties.getUser()).thenReturn("USER");
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> authService.register(registerRequest));

        assertEquals("Default role not configured", ex.getMessage());
    }

    @Test
    void register_whenValidRequest_createsUserWithDefaultRole() {
        Role userRole = new Role("USER", "Standard user");
        when(userService.existsByEmail("sam@example.com")).thenReturn(false);
        when(userService.existsByPhoneNumber("9876543210")).thenReturn(false);
        when(roleProperties.getUser()).thenReturn("USER");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));

        authService.register(registerRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("sam@example.com", saved.getEmail());
        assertEquals("9876543210", saved.getPhoneNumber());
        assertTrue(saved.getRoles().contains(userRole));
    }

    @Test
    void login_whenValidRequest_returnsTokensAndStoresRefreshTokenInRedis() {
        User user = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        setField(user, "tenantId", 1L);
        UserDetails userDetails = new TenantAwareUserDetails(
                10L,
                1L,
                "sam@example.com",
                "hash",
                true,
                java.util.Set.of(new SimpleGrantedAuthority("USER"))
        );

        when(userService.findByEmail("sam@example.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsernameAndTenantId("sam@example.com", 1L)).thenReturn(userDetails);
        when(jwtService.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token");
        when(jwtProperties.getAccessExpirationSeconds()).thenReturn(300L);
        when(jwtProperties.getRefreshExpirationSeconds()).thenReturn(1200L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponseDto response = authService.login(loginRequest);

        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("sam@example.com", "secret123"));
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(valueOperations).set(
                eq("refresh:1:refresh-token"),
                eq("1:sam@example.com"),
                eq(Duration.ofSeconds(1200L))
        );
    }

    @Test
    void refresh_whenTokenCannotBeParsed_throwsInvalidRefreshTokenException() {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto();
        request.setRefreshToken("bad-token");
        when(jwtService.extractUsername("bad-token")).thenThrow(new RuntimeException("invalid"));

        InvalidRefreshTokenException ex = assertThrows(InvalidRefreshTokenException.class,
                () -> authService.refresh(request));

        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void refresh_whenTokenNotInRedis_throwsInvalidRefreshTokenException() {
        String rawToken = "refresh-token";
        RefreshTokenRequestDto request = new RefreshTokenRequestDto();
        request.setRefreshToken(rawToken);

        User user = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        setField(user, "tenantId", 1L);
        UserDetails userDetails = new TenantAwareUserDetails(
                10L,
                1L,
                "sam@example.com",
                "hash",
                true,
                java.util.Set.of(new SimpleGrantedAuthority("USER"))
        );

        when(jwtService.extractUsername(rawToken)).thenReturn("sam@example.com");
        when(jwtService.extractTenantId(rawToken)).thenReturn(1L);
        when(userService.findByEmail("sam@example.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsernameAndTenantId("sam@example.com", 1L)).thenReturn(userDetails);
        when(jwtService.isRefreshTokenValid(rawToken, userDetails)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:1:refresh-token")).thenReturn(null);

        InvalidRefreshTokenException ex = assertThrows(InvalidRefreshTokenException.class,
                () -> authService.refresh(request));

        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void refresh_whenTokenValid_returnsNewAccessAndSameRefreshToken() {
        String rawToken = "refresh-token";
        RefreshTokenRequestDto request = new RefreshTokenRequestDto();
        request.setRefreshToken(rawToken);

        User user = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        setField(user, "tenantId", 1L);
        UserDetails userDetails = new TenantAwareUserDetails(
                10L,
                1L,
                "sam@example.com",
                "hash",
                true,
                java.util.Set.of(new SimpleGrantedAuthority("USER"))
        );

        when(jwtService.extractUsername(rawToken)).thenReturn("sam@example.com");
        when(jwtService.extractTenantId(rawToken)).thenReturn(1L);
        when(userService.findByEmail("sam@example.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsernameAndTenantId("sam@example.com", 1L)).thenReturn(userDetails);
        when(jwtService.isRefreshTokenValid(rawToken, userDetails)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:1:refresh-token")).thenReturn("1:sam@example.com");
        when(jwtService.generateAccessToken(userDetails)).thenReturn("new-access");
        when(jwtProperties.getAccessExpirationSeconds()).thenReturn(300L);
        when(jwtProperties.getRefreshExpirationSeconds()).thenReturn(1200L);

        AuthResponseDto response = authService.refresh(request);

        assertEquals("new-access", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    void logout_whenCalled_deletesRedisRefreshKey() {
        LogoutRequestDto request = new LogoutRequestDto();
        request.setRefreshToken("refresh-token");
        when(jwtService.extractTenantId("refresh-token")).thenReturn(1L);

        authService.logout(request);

        verify(redisTemplate).delete("refresh:1:refresh-token");
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
