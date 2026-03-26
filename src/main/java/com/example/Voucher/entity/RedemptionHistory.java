package com.example.Voucher.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "redemption_history", uniqueConstraints = @UniqueConstraint(name = "uc_redemption_history_tenant_request", columnNames = {
        "tenant_id",
        "request_id" }), indexes = @Index(name = "idx_redemption_history_tenant_id", columnList = "tenant_id"))
public class RedemptionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_voucher_id", nullable = false)
    private UserVoucher userVoucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal redeemedAmount;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBalanceAfter;

    @Column(nullable = false)
    private LocalDateTime redeemedAt;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    protected RedemptionHistory() {
    }

    public RedemptionHistory(UserVoucher userVoucher,
            Bill bill,
            BigDecimal redeemedAmount,
            BigDecimal remainingBalanceAfter,
            String requestId) {
        if (bill != null && !userVoucher.getTenantId().equals(bill.getTenantId())) {
            throw new IllegalArgumentException("Cross-tenant redemption history is not allowed");
        }
        this.userVoucher = userVoucher;
        this.bill = bill;
        this.redeemedAmount = redeemedAmount.setScale(2, RoundingMode.HALF_UP);
        this.remainingBalanceAfter = remainingBalanceAfter.setScale(2, RoundingMode.HALF_UP);
        this.redeemedAt = LocalDateTime.now();
        this.tenantId = userVoucher.getTenantId();
        this.requestId = requestId;
    }

    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public UserVoucher getUserVoucher() {
        return userVoucher;
    }

    public Bill getBill() {
        return bill;
    }

    public BigDecimal getRedeemedAmount() {
        return redeemedAmount;
    }

    public BigDecimal getRemainingBalanceAfter() {
        return remainingBalanceAfter;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }

    public Long getTenantId() {
        return tenantId;
    }
}
