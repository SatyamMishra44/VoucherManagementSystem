package com.example.Voucher.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "voucher_templates",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "code")
        }
)
public class VoucherTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String code;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitValue;

    @NotNull
    @Column(nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(nullable = false)
    private LocalDate expiryDate;

    @NotNull
    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected VoucherTemplate() {
    }

    public VoucherTemplate(String code,
                           BigDecimal unitValue,
                           LocalDate startDate,
                           LocalDate expiryDate) {
        this.code = code;
        this.unitValue = unitValue.setScale(2, RoundingMode.HALF_UP);
        this.startDate = startDate;
        this.expiryDate = expiryDate;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getUnitValue() {
        return unitValue;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
