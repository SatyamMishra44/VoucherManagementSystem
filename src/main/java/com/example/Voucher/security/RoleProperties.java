package com.example.Voucher.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.roles")
public class RoleProperties {
    private String platformAdmin;
    private String tenantAdmin;
    private String user;

    public String getPlatformAdmin() { return platformAdmin; }
    public void setPlatformAdmin(String platformAdmin) { this.platformAdmin = platformAdmin; }

    public String getTenantAdmin() { return tenantAdmin; }
    public void setTenantAdmin(String tenantAdmin) { this.tenantAdmin = tenantAdmin; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    // Backward-compatible alias for older SpEL usages.
    public String getAdmin() { return platformAdmin; }
}
