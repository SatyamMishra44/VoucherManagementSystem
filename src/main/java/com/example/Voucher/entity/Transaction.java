package com.example.Voucher.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

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

    @NotNull
    @Column(nullable = false)
    private Integer totalAmount; // before applying the voucher

    @NotNull
    @Column(nullable = false)
    private  Integer finalAmount;  // after applying the voucher

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Transaction(){
        // this is no-args constructor only needed for the JPA
    }
    public Transaction(User user,Integer totalAmount, Integer finalAmount){
        this.user = user;
        this.totalAmount = totalAmount;
        this.finalAmount = finalAmount;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId(){
        return  id;
    }
    public User getUser(){
        return user;
    }
    public  Integer getTotalAmount(){
        return totalAmount;
    }
    public Integer getFinalAmount(){
        return finalAmount;
    }
    public LocalDateTime getCreatedAt(){
        return createdAt;
    }


}
