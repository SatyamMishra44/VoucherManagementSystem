package com.example.Voucher.controller;

import com.example.Voucher.dto.platform.TenantAuditLogResponseDto;
import com.example.Voucher.dto.platform.TenantResponseDto;
import com.example.Voucher.dto.platform.TenantStatusUpdateRequestDto;
import com.example.Voucher.platform.PlatformTenantManagementService;
import com.example.Voucher.service.CurrentUserService;
import com.example.Voucher.tenant.Tenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "7. Platform Tenants", description = "Platform-only tenant onboarding and lifecycle management")
@RestController
@RequestMapping("/api/v1/platform/tenants")
public class PlatformTenantController {

    private final PlatformTenantManagementService platformTenantManagementService;
    private final CurrentUserService currentUserService;

    public PlatformTenantController(
            PlatformTenantManagementService platformTenantManagementService,
            CurrentUserService currentUserService
    ) {
        this.platformTenantManagementService = platformTenantManagementService;
        this.currentUserService = currentUserService;
    }

    @PatchMapping("/{tenantId}/status")
    @PreAuthorize("hasAuthority(@roleProperties.getPlatformAdmin())")
    @Operation(
            summary = "Platform: Activate/Deactivate Tenant",
            description = "Changes tenant lifecycle status and records a platform audit log entry."
    )
    public ResponseEntity<TenantResponseDto> updateTenantStatus(
            @PathVariable Long tenantId,
            @Valid @RequestBody TenantStatusUpdateRequestDto request
    ) {
        Tenant tenant = platformTenantManagementService.updateTenantActiveStatus(
                tenantId,
                Boolean.TRUE.equals(request.getActive()),
                request.getReason(),
                currentUserService.getCurrentUserId()
        );
        return ResponseEntity.ok(TenantResponseDto.fromEntity(tenant));
    }

    @GetMapping
    @PreAuthorize("hasAuthority(@roleProperties.getPlatformAdmin())")
    @Operation(
            summary = "Platform: List Tenants",
            description = "Lists all tenants (SYSTEM_INDIVIDUAL and ORGANIZATION) for platform governance."
    )
    public ResponseEntity<List<TenantResponseDto>> listTenants() {
        List<TenantResponseDto> response = platformTenantManagementService.listTenants()
                .stream()
                .map(TenantResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tenantId}/audit-logs")
    @PreAuthorize("hasAuthority(@roleProperties.getPlatformAdmin())")
    @Operation(
            summary = "Platform: Tenant Audit Trail",
            description = "Returns latest tenant lifecycle/onboarding audit logs for traceability."
    )
    public ResponseEntity<List<TenantAuditLogResponseDto>> getTenantAuditLogs(@PathVariable Long tenantId) {
        List<TenantAuditLogResponseDto> response = platformTenantManagementService.getTenantAuditLogs(tenantId)
                .stream()
                .map(TenantAuditLogResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
