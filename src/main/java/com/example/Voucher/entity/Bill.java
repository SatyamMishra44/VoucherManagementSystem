package com.example.Voucher.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "bills", uniqueConstraints = @UniqueConstraint(name = "uc_bills_tenant_request", columnNames = {
        "tenant_id", "request_id" }), indexes = @Index(name = "idx_bills_tenant_id", columnList = "tenant_id"))
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", length = 100)
    private String requestId;

    // Many bills -> one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // JPA requirement
    protected Bill() {
    }

    public Bill(User user, BigDecimal totalAmount, String requestId) {
        this.user = user;
        this.totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        this.createdAt = LocalDateTime.now();
        this.tenantId = user != null ? user.getTenantId() : null;
        this.requestId = requestId;
    }

    // Getters only
    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getTenantId() {
        return tenantId;
    }
}
