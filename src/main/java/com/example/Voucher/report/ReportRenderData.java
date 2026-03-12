package com.example.Voucher.report;

import com.example.Voucher.entity.Transaction;
import com.example.Voucher.entity.UserVoucher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportRenderData {

    private final String title;
    private final String scopeLabel;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final String generatedAt;
    private final long totalTransactions;
    private final BigDecimal grossAmount;
    private final BigDecimal finalAmount;
    private final BigDecimal redeemedAmount;
    private final long totalVouchers;
    private final long fullyRedeemedVouchers;
    private final long partiallyRedeemedVouchers;
    private final long notRedeemedVouchers;
    private final long activeVouchers;
    private final long inactiveVouchers;
    private final BigDecimal totalVoucherPurchasedAmount;
    private final BigDecimal totalVoucherRemainingAmount;
    private final List<Transaction> transactions;
    private final List<UserVoucher> vouchers;

    public ReportRenderData(
            String title,
            String scopeLabel,
            LocalDate fromDate,
            LocalDate toDate,
            String generatedAt,
            long totalTransactions,
            BigDecimal grossAmount,
            BigDecimal finalAmount,
            BigDecimal redeemedAmount,
            long totalVouchers,
            long fullyRedeemedVouchers,
            long partiallyRedeemedVouchers,
            long notRedeemedVouchers,
            long activeVouchers,
            long inactiveVouchers,
            BigDecimal totalVoucherPurchasedAmount,
            BigDecimal totalVoucherRemainingAmount,
            List<Transaction> transactions,
            List<UserVoucher> vouchers
    ) {
        this.title = title;
        this.scopeLabel = scopeLabel;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.generatedAt = generatedAt;
        this.totalTransactions = totalTransactions;
        this.grossAmount = grossAmount;
        this.finalAmount = finalAmount;
        this.redeemedAmount = redeemedAmount;
        this.totalVouchers = totalVouchers;
        this.fullyRedeemedVouchers = fullyRedeemedVouchers;
        this.partiallyRedeemedVouchers = partiallyRedeemedVouchers;
        this.notRedeemedVouchers = notRedeemedVouchers;
        this.activeVouchers = activeVouchers;
        this.inactiveVouchers = inactiveVouchers;
        this.totalVoucherPurchasedAmount = totalVoucherPurchasedAmount;
        this.totalVoucherRemainingAmount = totalVoucherRemainingAmount;
        this.transactions = transactions;
        this.vouchers = vouchers;
    }

    public String getTitle() {
        return title;
    }

    public String getScopeLabel() {
        return scopeLabel;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public BigDecimal getRedeemedAmount() {
        return redeemedAmount;
    }

    public long getTotalVouchers() {
        return totalVouchers;
    }

    public long getFullyRedeemedVouchers() {
        return fullyRedeemedVouchers;
    }

    public long getPartiallyRedeemedVouchers() {
        return partiallyRedeemedVouchers;
    }

    public long getNotRedeemedVouchers() {
        return notRedeemedVouchers;
    }

    public long getActiveVouchers() {
        return activeVouchers;
    }

    public long getInactiveVouchers() {
        return inactiveVouchers;
    }

    public BigDecimal getTotalVoucherPurchasedAmount() {
        return totalVoucherPurchasedAmount;
    }

    public BigDecimal getTotalVoucherRemainingAmount() {
        return totalVoucherRemainingAmount;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public List<UserVoucher> getVouchers() {
        return vouchers;
    }
}

