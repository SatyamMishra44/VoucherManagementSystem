package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.User;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void findById_usesTenantId() {
        User user = buildUser();
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findById(10L);

        assertTrue(result.isPresent());
        assertSame(user, result.orElseThrow());
        verify(userRepository).findByIdAndTenantId(10L, 1L);
    }

    @Test
    void createUser_encodesPassword_setsTenantId_andSaves() {
        User user = buildUser();
        user.setPasswordHash("plain");
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(user)).thenReturn(user);

        User saved = userService.createUser(user);

        assertSame(user, saved);
        assertEquals("encoded", user.getPasswordHash());
        assertEquals(1L, user.getTenantId());
        verify(passwordEncoder).encode("plain");
        verify(userRepository).save(user);
    }

    @Test
    void existsByEmail_usesTenantId() {
        when(userRepository.existsByEmailAndTenantId("a@b.com", 1L)).thenReturn(true);

        boolean result = userService.existsByEmail("a@b.com");

        assertTrue(result);
        verify(userRepository).existsByEmailAndTenantId("a@b.com", 1L);
    }

    @Test
    void existsByPhoneNumber_usesTenantId() {
        when(userRepository.existsByPhoneNumberAndTenantId("1234567890", 1L)).thenReturn(true);

        boolean result = userService.existsByPhoneNumber("1234567890");

        assertTrue(result);
        verify(userRepository).existsByPhoneNumberAndTenantId("1234567890", 1L);
    }

    @Test
    void getAllUsers_usesTenantId() {
        List<User> users = List.of(buildUser());
        when(userRepository.findAllByTenantId(1L)).thenReturn(users);

        List<User> result = userService.getAllUsers();

        assertSame(users, result);
        verify(userRepository).findAllByTenantId(1L);
    }

    @Test
    void getUsersWithFilters_allNullOrBlank_callsGetAllUsers() {
        List<User> users = List.of(buildUser());
        when(userRepository.findAllByTenantId(1L)).thenReturn(users);

        List<User> result = userService.getUsersWithFilters(" ", null, "", "   ", null);

        assertSame(users, result);
        verify(userRepository).findAllByTenantId(1L);
        verify(userRepository, never()).findAllByTenantIdWithFilters(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void getUsersWithFilters_trimsAndPassesNormalizedValues() {
        List<User> users = List.of(buildUser());
        when(userRepository.findAllByTenantIdWithFilters(
                1L,
                "Alice",
                null,
                "bob@example.com",
                "1234567890",
                true
        )).thenReturn(users);

        List<User> result = userService.getUsersWithFilters(
                "  Alice  ",
                "   ",
                "  bob@example.com ",
                " 1234567890 ",
                true
        );

        assertSame(users, result);
        verify(userRepository).findAllByTenantIdWithFilters(
                1L,
                "Alice",
                null,
                "bob@example.com",
                "1234567890",
                true
        );
    }

    @Test
    void existsById_usesTenantId() {
        when(userRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(true);

        boolean result = userService.existsById(99L);

        assertTrue(result);
        verify(userRepository).existsByIdAndTenantId(99L, 1L);
    }

    @Test
    void findByEmail_usesTenantId() {
        User user = buildUser();
        when(userRepository.findByEmailAndTenantId("x@y.com", 1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("x@y.com");

        assertTrue(result.isPresent());
        assertSame(user, result.orElseThrow());
        verify(userRepository).findByEmailAndTenantId("x@y.com", 1L);
    }

    private User buildUser() {
        User user = new User(
                "Test",
                "User",
                "hash",
                "1234567890",
                "test@example.com",
                LocalDateTime.now()
        );
        user.setTenantId(1L);
        return user;
    }
}
