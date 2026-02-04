package com.example.Voucher.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "voucher_redemptions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"voucher_id", "user_id"})
        }
)
public class VoucherRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //many redemption -> one voucher
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id",nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="transaction_id")
    private Transaction transaction;

    @NotNull
    @Column(nullable = false)
    private Integer discountApplied;

    @Column(nullable = false)
    private LocalDateTime redeemedAt;

    protected VoucherRedemption(){
     // this is only for the JPA
    }
    public VoucherRedemption(
            User user,
            Voucher voucher,
            Transaction transaction,
            Integer discountApplied
    ) {
        if (user == null || voucher == null || transaction == null) {
            throw new IllegalArgumentException("User, voucher, and transaction must not be null");
        }

        if (discountApplied == null || discountApplied < 0) {
            throw new IllegalArgumentException("Discount applied must be zero or positive");
        }

        this.user = user;
        this.voucher = voucher;
        this.transaction = transaction;
        this.discountApplied = discountApplied;
        this.redeemedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Voucher getVoucher() {
        return voucher;
    }

    public User getUser() {
        return user;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public @NotNull Integer getDiscountApplied() {
        return discountApplied;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }
}
