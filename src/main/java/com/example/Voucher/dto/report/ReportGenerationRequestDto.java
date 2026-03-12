package com.example.Voucher.dto.report;

import com.example.Voucher.report.ReportTargetType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class ReportGenerationRequestDto {

    @NotNull
    private ReportTargetType targetType;

    @NotNull
    private Long targetId;

    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    @Email
    private String recipientEmail;

    private Boolean includeTenantAdmins = true;

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(ReportTargetType targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public Boolean getIncludeTenantAdmins() {
        return includeTenantAdmins;
    }

    public void setIncludeTenantAdmins(Boolean includeTenantAdmins) {
        this.includeTenantAdmins = includeTenantAdmins;
    }
}

