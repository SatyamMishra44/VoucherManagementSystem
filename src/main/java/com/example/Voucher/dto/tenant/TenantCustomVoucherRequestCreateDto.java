package com.example.Voucher.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TenantCustomVoucherRequestCreateDto {

    @NotBlank(message = "Requested voucher code is required")
    private String requestedVoucherCode;

    @NotNull(message = "Requested unit value is required")
    @Positive(message = "Requested unit value must be greater than zero")
    private BigDecimal requestedUnitValue;

    @NotNull(message = "Requested start date is required")
    private LocalDate requestedStartDate;

    @NotNull(message = "Requested expiry date is required")
    private LocalDate requestedExpiryDate;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;

    public String getRequestedVoucherCode() {
        return requestedVoucherCode;
    }

    public void setRequestedVoucherCode(String requestedVoucherCode) {
        this.requestedVoucherCode = requestedVoucherCode;
    }

    public BigDecimal getRequestedUnitValue() {
        return requestedUnitValue;
    }

    public void setRequestedUnitValue(BigDecimal requestedUnitValue) {
        this.requestedUnitValue = requestedUnitValue;
    }

    public LocalDate getRequestedStartDate() {
        return requestedStartDate;
    }

    public void setRequestedStartDate(LocalDate requestedStartDate) {
        this.requestedStartDate = requestedStartDate;
    }

    public LocalDate getRequestedExpiryDate() {
        return requestedExpiryDate;
    }

    public void setRequestedExpiryDate(LocalDate requestedExpiryDate) {
        this.requestedExpiryDate = requestedExpiryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
