package com.example.Voucher.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", uniqueConstraints = @UniqueConstraint(name = "uc_transactions_tenant_request", columnNames = {
        "tenant_id", "request_id" }), indexes = @Index(name = "idx_transactions_tenant_id", columnList = "tenant_id"))
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount; // before applying the voucher

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal finalAmount; // after applying the voucher

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    protected Transaction() {
        // this is no-args constructor only needed for the JPA
    }

    public Transaction(User user, Bill bill, BigDecimal totalAmount, BigDecimal finalAmount, String requestId) {
        if (!user.getTenantId().equals(bill.getTenantId())) {
            throw new IllegalArgumentException("Cross-tenant transaction is not allowed");
        }
        this.user = user;
        this.bill = bill;
        this.totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        this.finalAmount = finalAmount.setScale(2, RoundingMode.HALF_UP);
        this.createdAt = LocalDateTime.now();
        this.tenantId = user.getTenantId();
        this.requestId = requestId;
    }

    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public User getUser() {
        return user;
    }

    public Bill getBill() {
        return bill;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getTenantId() {
        return tenantId;
    }

}
