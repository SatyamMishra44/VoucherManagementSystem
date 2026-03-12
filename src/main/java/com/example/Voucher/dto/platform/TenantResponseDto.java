package com.example.Voucher.dto.platform;

import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantType;
import java.time.LocalDateTime;

public class TenantResponseDto {

    private Long id;
    private String tenantCode;
    private String tenantName;
    private TenantType tenantType;
    private boolean active;
    private LocalDateTime createdAt;

    public static TenantResponseDto fromEntity(Tenant tenant) {
        TenantResponseDto dto = new TenantResponseDto();
        dto.id = tenant.getId();
        dto.tenantCode = tenant.getTenantCode();
        dto.tenantName = tenant.getTenantName();
        dto.tenantType = tenant.getTenantType();
        dto.active = tenant.isActive();
        dto.createdAt = tenant.getCreatedAt();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public String getTenantName() {
        return tenantName;
    }

    public TenantType getTenantType() {
        return tenantType;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
