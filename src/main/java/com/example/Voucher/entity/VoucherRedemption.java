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
    public VoucherRedemption(Voucher voucher, User user, Transaction transaction, Integer discountApplied, LocalDateTime redeemedAt) {
        this.voucher = voucher;
        this.user = user;
        this.transaction = transaction;
        this.discountApplied = discountApplied;
        this.redeemedAt = redeemedAt;
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
