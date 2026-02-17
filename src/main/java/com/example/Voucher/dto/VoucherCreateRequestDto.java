package com.example.Voucher.dto;

import com.example.Voucher.entity.User;
import com.example.Voucher.entity.Voucher;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class VoucherCreateRequestDto {
    @NotBlank(message = "Voucher code is required")
    @Size(min = 3, max = 20, message = "Voucher code must be between 3 and 20 characters")
    private String code;

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount percentage must be greater than 0")
    @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100")
    private double discountPercentage;

    @NotNull(message = "Minimum bill amount is required")
    @PositiveOrZero(message = "Minimum bill amount cannot be negative")
    private BigDecimal minBillAmount;

    //@Positive(message = "Maximum discount amount must be positive")
    //private Double maxDiscountAmount; // optional cap

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @NotNull(message = "Assigned user id is required")
    @Positive(message = "Assigned user id must be greater than zero")
    private Long userId;

    public Voucher toEntity(User createdBy, User assignedUser) {
        return new Voucher(
                this.code,
                this.discountPercentage,
                this.minBillAmount,
                this.startDate,
                this.expiryDate,
                assignedUser,
                createdBy
        );
    }
}
