package com.example.Voucher.dto.platform;

import com.example.Voucher.platform.TenantAuditLog;
import java.time.LocalDateTime;

public class TenantAuditLogResponseDto {

    private Long id;
    private Long tenantId;
    private Long actorUserId;
    private String action;
    private String details;
    private LocalDateTime createdAt;

    public static TenantAuditLogResponseDto fromEntity(TenantAuditLog log) {
        TenantAuditLogResponseDto dto = new TenantAuditLogResponseDto();
        dto.id = log.getId();
        dto.tenantId = log.getTenantId();
        dto.actorUserId = log.getActorUserId();
        dto.action = log.getAction();
        dto.details = log.getDetails();
        dto.createdAt = log.getCreatedAt();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
