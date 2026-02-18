package com.example.Voucher.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "redemption_history")
public class RedemptionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    protected RedemptionHistory() {
    }

    public RedemptionHistory(UserVoucher userVoucher,
                             Bill bill,
                             BigDecimal redeemedAmount,
                             BigDecimal remainingBalanceAfter) {
        this.userVoucher = userVoucher;
        this.bill = bill;
        this.redeemedAmount = redeemedAmount.setScale(2, RoundingMode.HALF_UP);
        this.remainingBalanceAfter = remainingBalanceAfter.setScale(2, RoundingMode.HALF_UP);
        this.redeemedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
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
}
