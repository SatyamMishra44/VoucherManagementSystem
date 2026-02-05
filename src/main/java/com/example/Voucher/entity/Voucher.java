package com.example.Voucher.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
    @Column(nullable = false)
    private Integer maxGlobalUses;

    @NotNull
    @Column(nullable = false)
    private Integer usedCount = 0;


    @NotNull
    @Column(nullable = false)
    private Boolean isEnabled = true;

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
            Integer maxGlobalUses,
            User createdBy
    ) {
        this.code = code;
        this.discountPercentage = discountPercentage;
        this.maxGlobalUses = maxGlobalUses;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.usedCount = 0;
        this.isEnabled = true;
    }

    // Getters
    public Long getId() { return id; }
    public String getCode() { return code; }
    public double getDiscountPercentage() { return discountPercentage; }
    public Integer getMaxGlobalUses() { return maxGlobalUses; }
    public Integer getUsedCount() { return usedCount; }
    public Boolean getIsEnabled() { return isEnabled; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getModifiedBy() { return modifiedBy; }
    public LocalDateTime getModifiedAt() { return modifiedAt; }

    // Domain behavior
    public void incrementUsage() {
        this.usedCount++;
    }


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

    public boolean hasExceededUsageLimit() {
        return maxGlobalUses !=null && usedCount >= maxGlobalUses;
    }
}
