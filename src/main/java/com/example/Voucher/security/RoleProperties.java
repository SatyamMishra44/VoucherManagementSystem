package com.example.Voucher.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.roles")
public class RoleProperties {
    private String admin;
    private String user;

    public String getAdmin() { return admin; }
    public void setAdmin(String admin) { this.admin = admin; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
}
