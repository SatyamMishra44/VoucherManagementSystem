package com.example.Voucher.dto.platform;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TenantStatusUpdateRequestDto {

    @NotNull(message = "Active flag is required")
    private Boolean active;

    @Size(max = 500, message = "Reason must be at most 500 characters")
    private String reason;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
