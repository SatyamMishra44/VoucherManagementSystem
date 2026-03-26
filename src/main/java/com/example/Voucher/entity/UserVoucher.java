package com.example.Voucher.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_vouchers", uniqueConstraints = @UniqueConstraint(name = "uc_user_vouchers_tenant_request", columnNames = {
        "tenant_id", "request_id" }), indexes = @Index(name = "idx_user_vouchers_tenant_id", columnList = "tenant_id"))
public class UserVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_template_id", nullable = false)
    private VoucherTemplate voucherTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(nullable = false)
    private Integer quantityPurchased;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPurchasedAmount;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBalance;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserVoucherStatus status;

    @Column(nullable = false)
    private LocalDateTime purchasedAt;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    protected UserVoucher() {
    }

    public UserVoucher(VoucherTemplate voucherTemplate,
            User user,
            Integer quantityPurchased,
            BigDecimal totalPurchasedAmount,
            BigDecimal remainingBalance,
            String requestId) {
        this.voucherTemplate = voucherTemplate;
        this.user = user;
        this.quantityPurchased = quantityPurchased;
        this.totalPurchasedAmount = totalPurchasedAmount.setScale(2, RoundingMode.HALF_UP);
        this.remainingBalance = remainingBalance.setScale(2, RoundingMode.HALF_UP);
        this.status = UserVoucherStatus.ACTIVE;
        this.purchasedAt = LocalDateTime.now();
        this.tenantId = user.getTenantId();
        this.requestId = requestId;
    }

    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public VoucherTemplate getVoucherTemplate() {
        return voucherTemplate;
    }

    public User getUser() {
        return user;
    }

    public Integer getQuantityPurchased() {
        return quantityPurchased;
    }

    public BigDecimal getTotalPurchasedAmount() {
        return totalPurchasedAmount;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public UserVoucherStatus getStatus() {
        return status;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public boolean isActive() {
        return UserVoucherStatus.ACTIVE.equals(status);
    }

    public void applyRedemption(BigDecimal redeemedAmount) {
        BigDecimal normalized = redeemedAmount.setScale(2, RoundingMode.HALF_UP);
        this.remainingBalance = this.remainingBalance.subtract(normalized).setScale(2, RoundingMode.HALF_UP);
        if (this.remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            this.remainingBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.status = UserVoucherStatus.INACTIVE;
        }
    }
}
