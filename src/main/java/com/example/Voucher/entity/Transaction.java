package com.example.Voucher.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount; // before applying the voucher

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal finalAmount;  // after applying the voucher

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Transaction(){
        // this is no-args constructor only needed for the JPA
    }
    public Transaction(User user, Bill bill, BigDecimal totalAmount, BigDecimal finalAmount){
        this.user = user;
        this.bill = bill;
        this.totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        this.finalAmount = finalAmount.setScale(2, RoundingMode.HALF_UP);
        this.createdAt = LocalDateTime.now();
    }

    public Long getId(){
        return  id;
    }
    public User getUser(){
        return user;
    }
    public Bill getBill() {
        return bill;
    }
    public BigDecimal getTotalAmount(){
        return totalAmount;
    }
    public BigDecimal getFinalAmount(){
        return finalAmount;
    }
    public LocalDateTime getCreatedAt(){
        return createdAt;
    }


}
