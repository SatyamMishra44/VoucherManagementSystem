package com.example.Voucher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_onboarding_requests")
public class TenantOnboardingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_name", nullable = false, length = 255)
    private String tenantName;

    @Column(name = "tenant_code", nullable = false, length = 100)
    private String tenantCode;

    @Column(name = "admin_first_name", nullable = false, length = 255)
    private String adminFirstName;

    @Column(name = "admin_last_name", nullable = false, length = 255)
    private String adminLastName;

    @Column(name = "admin_email", nullable = false, length = 255)
    private String adminEmail;

    @Column(name = "admin_phone_number", nullable = false, length = 20)
    private String adminPhoneNumber;

    @Column(name = "admin_password_hash", nullable = false, length = 255)
    private String adminPasswordHash;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TenantOnboardingStatus status;

    @Column(name = "review_comment", length = 2000)
    private String reviewComment;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "approved_tenant_id")
    private Long approvedTenantId;

    @Column(name = "approved_tenant_admin_user_id")
    private Long approvedTenantAdminUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected TenantOnboardingRequest() {
    }

    public TenantOnboardingRequest(
            String tenantName,
            String tenantCode,
            String adminFirstName,
            String adminLastName,
            String adminEmail,
            String adminPhoneNumber,
            String adminPasswordHash,
            String notes
    ) {
        this.tenantName = tenantName;
        this.tenantCode = tenantCode;
        this.adminFirstName = adminFirstName;
        this.adminLastName = adminLastName;
        this.adminEmail = adminEmail;
        this.adminPhoneNumber = adminPhoneNumber;
        this.adminPasswordHash = adminPasswordHash;
        this.notes = notes;
        this.status = TenantOnboardingStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
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

    public String getAdminPasswordHash() {
        return adminPasswordHash;
    }

    public String getNotes() {
        return notes;
    }

    public TenantOnboardingStatus getStatus() {
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

    public void markApproved(Long actorUserId, String reviewComment, Long tenantId, Long tenantAdminUserId) {
        this.status = TenantOnboardingStatus.APPROVED;
        this.reviewedByUserId = actorUserId;
        this.reviewComment = reviewComment;
        this.reviewedAt = LocalDateTime.now();
        this.approvedTenantId = tenantId;
        this.approvedTenantAdminUserId = tenantAdminUserId;
    }

    public void markRejected(Long actorUserId, String reviewComment) {
        this.status = TenantOnboardingStatus.REJECTED;
        this.reviewedByUserId = actorUserId;
        this.reviewComment = reviewComment;
        this.reviewedAt = LocalDateTime.now();
    }
}
