package com.example.Voucher.controller;

import com.example.Voucher.dto.onboarding.TenantOnboardingDecisionRequestDto;
import com.example.Voucher.dto.onboarding.TenantOnboardingRequestResponseDto;
import com.example.Voucher.entity.TenantOnboardingStatus;
import com.example.Voucher.service.CurrentUserService;
import com.example.Voucher.service.TenantOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "10. Platform Onboarding Review", description = "Platform admin APIs to approve or reject tenant self-signup requests")
@RestController
@RequestMapping("/api/v1/platform/onboarding")
public class PlatformOnboardingReviewController {

    private final TenantOnboardingService tenantOnboardingService;
    private final CurrentUserService currentUserService;

    public PlatformOnboardingReviewController(
            TenantOnboardingService tenantOnboardingService,
            CurrentUserService currentUserService
    ) {
        this.tenantOnboardingService = tenantOnboardingService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAuthority(@roleProperties.getPlatformAdmin())")
    @Operation(
            summary = "Platform: List Tenant Signup Requests",
            description = "Returns onboarding requests. Optional status filter: PENDING, APPROVED, REJECTED."
    )
    public ResponseEntity<List<TenantOnboardingRequestResponseDto>> listRequests(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(tenantOnboardingService.listRequests(parseStatus(status)));
    }

    @GetMapping("/requests/{requestId}")
    @PreAuthorize("hasAuthority(@roleProperties.getPlatformAdmin())")
    @Operation(
            summary = "Platform: Get Signup Request Details",
            description = "Returns full details of one onboarding request."
    )
    public ResponseEntity<TenantOnboardingRequestResponseDto> getRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(tenantOnboardingService.getRequest(requestId));
    }

    @PatchMapping("/requests/{requestId}/decision")
    @PreAuthorize("hasAuthority(@roleProperties.getPlatformAdmin())")
    @Operation(
            summary = "Platform: Approve or Reject Signup Request",
            description = "Approve creates tenant + tenant admin. Reject closes request with comment."
    )
    public ResponseEntity<TenantOnboardingRequestResponseDto> reviewRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody TenantOnboardingDecisionRequestDto request
    ) {
        TenantOnboardingRequestResponseDto response = tenantOnboardingService.reviewRequest(
                requestId,
                Boolean.TRUE.equals(request.getApproved()),
                request.getComment(),
                currentUserService.getCurrentUserId()
        );
        return ResponseEntity.ok(response);
    }

    private TenantOnboardingStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return TenantOnboardingStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status. Allowed: PENDING, APPROVED, REJECTED");
        }
    }
}
