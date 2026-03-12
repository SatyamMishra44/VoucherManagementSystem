package com.example.Voucher.dto.tenant;

import com.example.Voucher.entity.TenantVoucherRequest;
import com.example.Voucher.entity.TenantVoucherRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TenantCustomVoucherRequestResponseDto {

    private Long requestId;
    private Long tenantId;
    private Long requestedByUserId;
    private String requestedVoucherCode;
    private BigDecimal requestedUnitValue;
    private LocalDate requestedStartDate;
    private LocalDate requestedExpiryDate;
    private String notes;
    private TenantVoucherRequestStatus status;
    private LocalDateTime createdAt;

    public static TenantCustomVoucherRequestResponseDto fromEntity(TenantVoucherRequest request) {
        TenantCustomVoucherRequestResponseDto dto = new TenantCustomVoucherRequestResponseDto();
        dto.requestId = request.getId();
        dto.tenantId = request.getTenantId();
        dto.requestedByUserId = request.getRequestedByUser().getId();
        dto.requestedVoucherCode = request.getRequestedVoucherCode();
        dto.requestedUnitValue = request.getRequestedUnitValue();
        dto.requestedStartDate = request.getRequestedStartDate();
        dto.requestedExpiryDate = request.getRequestedExpiryDate();
        dto.notes = request.getNotes();
        dto.status = request.getStatus();
        dto.createdAt = request.getCreatedAt();
        return dto;
    }

    public Long getRequestId() {
        return requestId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getRequestedByUserId() {
        return requestedByUserId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
