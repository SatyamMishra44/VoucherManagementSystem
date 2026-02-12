package com.example.Voucher.service;

import com.example.Voucher.entity.User;
import com.example.Voucher.security.RoleProperties;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserService userService;
    private final RoleProperties roleProperties;

    public CurrentUserService(UserService userService, RoleProperties roleProperties) {
        this.userService = userService;
        this.roleProperties = roleProperties;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        return userService.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public void assertSelfOrAdmin(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), roleProperties.getAdmin()));
        if (isAdmin) {
            return;
        }

        String email = auth.getName();
        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
        if (!Objects.equals(currentUser.getId(), userId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
