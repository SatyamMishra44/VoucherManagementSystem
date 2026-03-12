package com.example.Voucher.controller;

import com.example.Voucher.dto.onboarding.TenantOnboardingRequestResponseDto;
import com.example.Voucher.dto.onboarding.TenantSignupRequestDto;
import com.example.Voucher.service.TenantOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "9. Tenant Onboarding", description = "Public tenant self-signup request APIs")
@RestController
@RequestMapping("/api/v1/tenant-onboarding")
public class TenantOnboardingController {

    private final TenantOnboardingService tenantOnboardingService;

    public TenantOnboardingController(TenantOnboardingService tenantOnboardingService) {
        this.tenantOnboardingService = tenantOnboardingService;
    }

    @PostMapping("/requests")
    @Operation(
            summary = "Submit Tenant Signup Request",
            description = "Collects organization and tenant-admin details and creates a PENDING onboarding request for platform approval."
    )
    public ResponseEntity<TenantOnboardingRequestResponseDto> submitOnboardingRequest(
            @Valid @RequestBody TenantSignupRequestDto request
    ) {
        TenantOnboardingRequestResponseDto response = tenantOnboardingService.submitOnboardingRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
