package com.example.Voucher.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "vouchers",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "code")
        }
)
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String code;

    @NotNull
    @Min(value = 1, message = "Discount percentage must be at least 1")
    @Max(value = 100, message = "Discount percentage cannot exceed 100")
    @Column(nullable = false)
    private double discountPercentage;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal minBillAmount;

    @NotNull
    @Column(nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(nullable = false)
    private LocalDate expiryDate;

    @NotNull
    @Column(nullable = false)
    private Boolean isEnabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id", nullable = false)
    private User assignedUser;

    @NotNull
    @Column(nullable = false)
    private Boolean isRedeemed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redeemed_bill_id")
    private Bill redeemedBill;

    private LocalDateTime redeemedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private User modifiedBy;

    private LocalDateTime modifiedAt;
    // ✅ Required by JPA
    protected Voucher() {
    }

    public Voucher(
            String code,
            double discountPercentage,
            BigDecimal minBillAmount,
            LocalDate startDate,
            LocalDate expiryDate,
            User assignedUser,
            User createdBy
    ) {
        this.code = code;
        this.discountPercentage = discountPercentage;
        this.minBillAmount = minBillAmount.setScale(2, RoundingMode.HALF_UP);
        this.startDate = startDate;
        this.expiryDate = expiryDate;
        this.assignedUser = assignedUser;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.isRedeemed = false;
        this.isEnabled = true;
    }

    // Getters
    public Long getId() { return id; }
    public String getCode() { return code; }
    public double getDiscountPercentage() { return discountPercentage; }
    public BigDecimal getMinBillAmount() { return minBillAmount; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public Boolean getIsEnabled() { return isEnabled; }
    public User getAssignedUser() { return assignedUser; }
    public Boolean getIsRedeemed() { return isRedeemed; }
    public Bill getRedeemedBill() { return redeemedBill; }
    public LocalDateTime getRedeemedAt() { return redeemedAt; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getModifiedBy() { return modifiedBy; }
    public LocalDateTime getModifiedAt() { return modifiedAt; }

    public void disable(User modifiedBy) {
        this.isEnabled = false;
        this.modifiedBy = modifiedBy;
        this.modifiedAt = LocalDateTime.now();
    }

    // avoid using setter for state changes reason and security aspect
    // this is the domain-driven state change not a setter
    public void enable(){
        this.isEnabled = true;
    }

    public void disable(){
        this.isEnabled = false;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isRedeemed() {
        return Boolean.TRUE.equals(isRedeemed);
    }

    public void markRedeemed(Bill bill) {
        if (bill == null) {
            throw new IllegalArgumentException("Bill must not be null");
        }
        this.isRedeemed = true;
        this.redeemedBill = bill;
        this.redeemedAt = LocalDateTime.now();
    }
}
