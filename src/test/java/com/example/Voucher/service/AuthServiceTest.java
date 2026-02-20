package com.example.Voucher.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

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
    private UserDetailsService userDetailsService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    private AuthRegisterRequestDto registerRequest;
    private AuthLoginRequestDto loginRequest;

    @BeforeEach
    void setUp() {
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
    void login_whenValidRequest_returnsTokensAndPersistsRefreshTokenHash() {
        User user = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("sam@example.com")
                .password("hash")
                .authorities("USER")
                .build();

        when(userService.findByEmail("sam@example.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("sam@example.com")).thenReturn(userDetails);
        when(jwtService.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token");
        when(jwtProperties.getAccessExpirationSeconds()).thenReturn(300L);
        when(jwtProperties.getRefreshExpirationSeconds()).thenReturn(1200L);

        AuthResponseDto response = authService.login(loginRequest);

        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("sam@example.com", "secret123"));
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertEquals(sha256("refresh-token"), tokenCaptor.getValue().getTokenHash());
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
    void refresh_whenTokenNotStored_throwsInvalidRefreshTokenException() {
        String rawToken = "refresh-token";
        RefreshTokenRequestDto request = new RefreshTokenRequestDto();
        request.setRefreshToken(rawToken);

        User user = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("sam@example.com")
                .password("hash")
                .authorities("USER")
                .build();

        when(jwtService.extractUsername(rawToken)).thenReturn("sam@example.com");
        when(userService.findByEmail("sam@example.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("sam@example.com")).thenReturn(userDetails);
        when(jwtService.isRefreshTokenValid(rawToken, userDetails)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.empty());

        InvalidRefreshTokenException ex = assertThrows(InvalidRefreshTokenException.class,
                () -> authService.refresh(request));

        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void refresh_whenStoredTokenRevoked_throwsInvalidRefreshTokenException() {
        String rawToken = "refresh-token";
        RefreshTokenRequestDto request = new RefreshTokenRequestDto();
        request.setRefreshToken(rawToken);

        User user = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("sam@example.com")
                .password("hash")
                .authorities("USER")
                .build();

        RefreshToken storedToken = new RefreshToken(user, sha256(rawToken), LocalDateTime.now().plusMinutes(10));
        storedToken.revoke(null);

        when(jwtService.extractUsername(rawToken)).thenReturn("sam@example.com");
        when(userService.findByEmail("sam@example.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("sam@example.com")).thenReturn(userDetails);
        when(jwtService.isRefreshTokenValid(rawToken, userDetails)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(storedToken));

        InvalidRefreshTokenException ex = assertThrows(InvalidRefreshTokenException.class,
                () -> authService.refresh(request));

        assertEquals("Refresh token expired or revoked", ex.getMessage());
    }

    @Test
    void refresh_whenValidToken_rotatesRefreshTokenAndReturnsNewTokenPair() {
        String oldRefresh = "old-refresh";
        String newRefresh = "new-refresh";

        RefreshTokenRequestDto request = new RefreshTokenRequestDto();
        request.setRefreshToken(oldRefresh);

        User user = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("sam@example.com")
                .password("hash")
                .authorities("USER")
                .build();

        RefreshToken storedToken = new RefreshToken(user, sha256(oldRefresh), LocalDateTime.now().plusMinutes(10));

        when(jwtService.extractUsername(oldRefresh)).thenReturn("sam@example.com");
        when(userService.findByEmail("sam@example.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("sam@example.com")).thenReturn(userDetails);
        when(jwtService.isRefreshTokenValid(oldRefresh, userDetails)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(sha256(oldRefresh))).thenReturn(Optional.of(storedToken));
        when(jwtService.generateRefreshToken(userDetails)).thenReturn(newRefresh);
        when(jwtService.generateAccessToken(userDetails)).thenReturn("new-access");
        when(jwtProperties.getAccessExpirationSeconds()).thenReturn(300L);
        when(jwtProperties.getRefreshExpirationSeconds()).thenReturn(1200L);

        AuthResponseDto response = authService.refresh(request);

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertTrue(storedToken.isRevoked());
        assertEquals(sha256(newRefresh), storedToken.getReplacedByTokenHash());

        ArgumentCaptor<RefreshToken> saveCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(saveCaptor.capture());
        assertEquals(storedToken, saveCaptor.getAllValues().get(0));
    }

    @Test
    void logout_whenTokenExistsAndNotRevoked_revokesAndSaves() {
        String rawToken = "refresh-token";
        LogoutRequestDto request = new LogoutRequestDto();
        request.setRefreshToken(rawToken);

        User user = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        RefreshToken stored = new RefreshToken(user, sha256(rawToken), LocalDateTime.now().plusMinutes(10));

        when(refreshTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(stored));

        authService.logout(request);

        assertTrue(stored.isRevoked());
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void logout_whenTokenAlreadyRevoked_doesNotSaveAgain() {
        String rawToken = "refresh-token";
        LogoutRequestDto request = new LogoutRequestDto();
        request.setRefreshToken(rawToken);

        User user = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        RefreshToken stored = new RefreshToken(user, sha256(rawToken), LocalDateTime.now().plusMinutes(10));
        stored.revoke(null);

        when(refreshTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(stored));

        authService.logout(request);

        verify(refreshTokenRepository, never()).save(stored);
        assertNotNull(stored.getRevokedAt());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
