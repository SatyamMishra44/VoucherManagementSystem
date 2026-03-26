package com.example.Voucher.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;



// tell user belongs to which tenant
public class TenantAwareUserDetails extends User {

    private final Long userId;
    private final Long tenantId;

    public TenantAwareUserDetails(
            Long userId,
            Long tenantId,
            String username,
            String password,
            boolean enabled,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(username, password, enabled, true, true, true, authorities);
        this.userId = userId; // extending the parent class object with help of super to initialize spring security user
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTenantId() {
        return tenantId;
    }
}
