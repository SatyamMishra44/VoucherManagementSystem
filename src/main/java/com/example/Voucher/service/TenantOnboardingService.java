package com.example.Voucher.service;

import com.example.Voucher.dto.onboarding.TenantOnboardingRequestResponseDto;
import com.example.Voucher.dto.onboarding.TenantSignupRequestDto;
import com.example.Voucher.entity.TenantOnboardingStatus;
import java.util.List;

public interface TenantOnboardingService {

    TenantOnboardingRequestResponseDto submitOnboardingRequest(TenantSignupRequestDto request);

    TenantOnboardingRequestResponseDto reviewRequest(Long requestId, boolean approved, String comment, Long actorUserId);

    List<TenantOnboardingRequestResponseDto> listRequests(TenantOnboardingStatus status);

    TenantOnboardingRequestResponseDto getRequest(Long requestId);
}
