package com.example.Voucher.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Voucher.entity.User;
import com.example.Voucher.security.RoleProperties;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private RoleProperties roleProperties;

    @InjectMocks
    private CurrentUserService currentUserService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_whenAuthenticationMissing_throwsAccessDeniedException() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> currentUserService.getCurrentUser());

        assertEquals("Authentication required", ex.getMessage());
    }

    @Test
    void getCurrentUser_whenUserNotFound_throwsAccessDeniedException() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("sam@example.com", "pwd", "USER")
        );
        when(userService.findByEmail("sam@example.com")).thenReturn(Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> currentUserService.getCurrentUser());

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void assertSelfOrAdmin_whenOtherUserWithoutAdminRole_throwsAccessDeniedException() {
        when(roleProperties.getPlatformAdmin()).thenReturn("PLATFORM_ADMIN");
        when(roleProperties.getTenantAdmin()).thenReturn("TENANT_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("sam@example.com", "pwd", "USER")
        );

        User currentUser = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        setField(currentUser, "id", 10L);
        when(userService.findByEmail("sam@example.com")).thenReturn(Optional.of(currentUser));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> currentUserService.assertSelfOrAdmin(99L));

        assertEquals("Access denied", ex.getMessage());
    }

    @Test
    void assertSelfOrAdmin_whenAdmin_skipsOwnershipCheck() {
        when(roleProperties.getPlatformAdmin()).thenReturn("ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin@example.com", "pwd", "ADMIN")
        );

        currentUserService.assertSelfOrAdmin(999L);

        verify(userService, never()).findByEmail("admin@example.com");
    }

    @Test
    void isCurrentUserAdmin_whenAuthorityPresent_returnsTrue() {
        when(roleProperties.getPlatformAdmin()).thenReturn("ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin@example.com", "pwd", "ADMIN")
        );

        boolean isAdmin = currentUserService.isCurrentUserAdmin();

        assertEquals(true, isAdmin);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
