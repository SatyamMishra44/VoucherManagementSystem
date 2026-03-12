package com.example.Voucher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tenant_voucher_distributions",
        indexes = @Index(name = "idx_tenant_voucher_distributions_tenant_id", columnList = "tenant_id")
)
public class TenantVoucherDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_voucher_inventory_id", nullable = false)
    private TenantVoucherInventory tenantVoucherInventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distributed_to_user_id", nullable = false)
    private User distributedToUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distributed_by_user_id", nullable = false)
    private User distributedByUser;

    @Column(name = "quantity_distributed", nullable = false)
    private Integer quantityDistributed;

    @Column(name = "total_distributed_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDistributedAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected TenantVoucherDistribution() {
    }

    public TenantVoucherDistribution(
            Long tenantId,
            TenantVoucherInventory tenantVoucherInventory,
            User distributedToUser,
            User distributedByUser,
            Integer quantityDistributed,
            BigDecimal totalDistributedAmount
    ) {
        this.tenantId = tenantId;
        this.tenantVoucherInventory = tenantVoucherInventory;
        this.distributedToUser = distributedToUser;
        this.distributedByUser = distributedByUser;
        this.quantityDistributed = quantityDistributed;
        this.totalDistributedAmount = totalDistributedAmount.setScale(2, RoundingMode.HALF_UP);
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public TenantVoucherInventory getTenantVoucherInventory() {
        return tenantVoucherInventory;
    }

    public User getDistributedToUser() {
        return distributedToUser;
    }

    public User getDistributedByUser() {
        return distributedByUser;
    }

    public Integer getQuantityDistributed() {
        return quantityDistributed;
    }

    public BigDecimal getTotalDistributedAmount() {
        return totalDistributedAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
