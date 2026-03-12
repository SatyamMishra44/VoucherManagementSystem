package com.example.Voucher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tenant_voucher_requests",
        indexes = @Index(name = "idx_tenant_voucher_requests_tenant_id", columnList = "tenant_id")
)
public class TenantVoucherRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedByUser;

    @Column(name = "requested_voucher_code", nullable = false)
    private String requestedVoucherCode;

    @Column(name = "requested_unit_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal requestedUnitValue;

    @Column(name = "requested_start_date", nullable = false)
    private LocalDate requestedStartDate;

    @Column(name = "requested_expiry_date", nullable = false)
    private LocalDate requestedExpiryDate;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TenantVoucherRequestStatus status;

    @Column(name = "platform_comment", length = 1000)
    private String platformComment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TenantVoucherRequest() {
    }

    public TenantVoucherRequest(
            Long tenantId,
            User requestedByUser,
            String requestedVoucherCode,
            BigDecimal requestedUnitValue,
            LocalDate requestedStartDate,
            LocalDate requestedExpiryDate,
            String notes
    ) {
        this.tenantId = tenantId;
        this.requestedByUser = requestedByUser;
        this.requestedVoucherCode = requestedVoucherCode;
        this.requestedUnitValue = requestedUnitValue.setScale(2, RoundingMode.HALF_UP);
        this.requestedStartDate = requestedStartDate;
        this.requestedExpiryDate = requestedExpiryDate;
        this.notes = notes;
        this.status = TenantVoucherRequestStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public User getRequestedByUser() {
        return requestedByUser;
    }

    public String getRequestedVoucherCode() {
        return requestedVoucherCode;
    }

    public BigDecimal getRequestedUnitValue() {
        return requestedUnitValue;
    }

    public LocalDate getRequestedStartDate() {
        return requestedStartDate;
    }

    public LocalDate getRequestedExpiryDate() {
        return requestedExpiryDate;
    }

    public String getNotes() {
        return notes;
    }

    public TenantVoucherRequestStatus getStatus() {
        return status;
    }

    public String getPlatformComment() {
        return platformComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
