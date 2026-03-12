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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tenant_voucher_inventory",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tenant_voucher_inventory_tenant_template",
                columnNames = {"tenant_id", "voucher_template_id"}
        ),
        indexes = @Index(name = "idx_tenant_voucher_inventory_tenant_id", columnList = "tenant_id")
)
public class TenantVoucherInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_template_id", nullable = false)
    private VoucherTemplate voucherTemplate;

    @Column(name = "quantity_purchased_total", nullable = false)
    private Integer quantityPurchasedTotal;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TenantVoucherInventory() {
    }

    public TenantVoucherInventory(Long tenantId, VoucherTemplate voucherTemplate, Integer initialQuantity) {
        this.tenantId = tenantId;
        this.voucherTemplate = voucherTemplate;
        this.quantityPurchasedTotal = initialQuantity;
        this.quantityAvailable = initialQuantity;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public VoucherTemplate getVoucherTemplate() {
        return voucherTemplate;
    }

    public Integer getQuantityPurchasedTotal() {
        return quantityPurchasedTotal;
    }

    public Integer getQuantityAvailable() {
        return quantityAvailable;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void addStock(Integer quantity) {
        this.quantityPurchasedTotal += quantity;
        this.quantityAvailable += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void consumeStock(Integer quantity) {
        if (quantityAvailable < quantity) {
            throw new IllegalArgumentException("Insufficient tenant voucher stock");
        }
        this.quantityAvailable -= quantity;
        this.updatedAt = LocalDateTime.now();
    }
}
