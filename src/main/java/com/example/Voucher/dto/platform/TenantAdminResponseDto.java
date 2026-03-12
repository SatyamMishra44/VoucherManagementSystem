package com.example.Voucher.dto.platform;

import java.time.LocalDateTime;

public class TenantAdminResponseDto {

    private Long userId;
    private Long tenantId;
    private String email;
    private String phoneNumber;
    private LocalDateTime createdAt;

    public TenantAdminResponseDto(Long userId, Long tenantId, String email, String phoneNumber, LocalDateTime createdAt) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
