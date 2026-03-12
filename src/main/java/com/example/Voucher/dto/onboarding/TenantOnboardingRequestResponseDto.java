package com.example.Voucher.dto.onboarding;

import com.example.Voucher.entity.TenantOnboardingRequest;
import java.time.LocalDateTime;

public class TenantOnboardingRequestResponseDto {

    private Long requestId;
    private String tenantName;
    private String tenantCode;
    private String adminFirstName;
    private String adminLastName;
    private String adminEmail;
    private String adminPhoneNumber;
    private String notes;
    private String status;
    private String reviewComment;
    private Long reviewedByUserId;
    private LocalDateTime reviewedAt;
    private Long approvedTenantId;
    private Long approvedTenantAdminUserId;
    private LocalDateTime createdAt;

    public static TenantOnboardingRequestResponseDto fromEntity(TenantOnboardingRequest request) {
        TenantOnboardingRequestResponseDto dto = new TenantOnboardingRequestResponseDto();
        dto.requestId = request.getId();
        dto.tenantName = request.getTenantName();
        dto.tenantCode = request.getTenantCode();
        dto.adminFirstName = request.getAdminFirstName();
        dto.adminLastName = request.getAdminLastName();
        dto.adminEmail = request.getAdminEmail();
        dto.adminPhoneNumber = request.getAdminPhoneNumber();
        dto.notes = request.getNotes();
        dto.status = request.getStatus().name();
        dto.reviewComment = request.getReviewComment();
        dto.reviewedByUserId = request.getReviewedByUserId();
        dto.reviewedAt = request.getReviewedAt();
        dto.approvedTenantId = request.getApprovedTenantId();
        dto.approvedTenantAdminUserId = request.getApprovedTenantAdminUserId();
        dto.createdAt = request.getCreatedAt();
        return dto;
    }

    public Long getRequestId() {
        return requestId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public String getAdminFirstName() {
        return adminFirstName;
    }

    public String getAdminLastName() {
        return adminLastName;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public String getAdminPhoneNumber() {
        return adminPhoneNumber;
    }

    public String getNotes() {
        return notes;
    }

    public String getStatus() {
        return status;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public Long getReviewedByUserId() {
        return reviewedByUserId;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public Long getApprovedTenantId() {
        return approvedTenantId;
    }

    public Long getApprovedTenantAdminUserId() {
        return approvedTenantAdminUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
