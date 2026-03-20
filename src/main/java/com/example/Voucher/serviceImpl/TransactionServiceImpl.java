package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Transaction;
import com.example.Voucher.repository.TransactionRepository;
import com.example.Voucher.service.TransactionService;
import com.example.Voucher.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;

    // Constructor injection
    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction createTransaction(Transaction transaction) {

        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        if (transaction.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero");
        }

        if (transaction.getFinalAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Final amount cannot be negative");
        }

        if (transaction.getFinalAmount().compareTo(transaction.getTotalAmount()) > 0) {
            throw new IllegalArgumentException("Final amount cannot exceed total amount");
        }

        log.info("action=createTransaction started | userId={} totalAmount={} finalAmount={}",
                transaction.getUser() != null ? transaction.getUser().getId() : null,
                transaction.getTotalAmount(), transaction.getFinalAmount());
        Transaction saved = transactionRepository.save(transaction);
        log.info(
                "action=createTransaction completed | transactionId={} userId={} totalAmount={} finalAmount={} discount={}",
                saved.getId(),
                saved.getUser() != null ? saved.getUser().getId() : null,
                saved.getTotalAmount(), saved.getFinalAmount(),
                saved.getTotalAmount().subtract(saved.getFinalAmount()));
        return saved;
    }

    @Override
    public Optional<Transaction> getTransactionById(Long transactionId) {

        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }

        log.debug("action=getTransactionById | transactionId={}", transactionId);
        Optional<Transaction> result = transactionRepository.findById(transactionId)
                .filter(transaction -> transaction.getTenantId().equals(TenantContext.requireTenantId()));
        if (result.isEmpty()) {
            log.warn("action=getTransactionById | transactionId={} result=not found or tenant mismatch", transactionId);
        }
        return result;
    }

    /**
     * Fetch all transactions for a user
     */
    @Override
    public List<Transaction> getTransactionByUserId(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        log.debug("action=getTransactionByUserId | userId={}", userId);
        List<Transaction> transactions = transactionRepository.findByUserIdAndTenantId(userId,
                TenantContext.requireTenantId());
        log.info("action=getTransactionByUserId completed | userId={} resultCount={}", userId, transactions.size());
        return transactions;
    }

    @Override
    public List<Transaction> getTransactionsWithFilters(
            Long userId,
            BigDecimal minTotalAmount,
            BigDecimal maxTotalAmount,
            BigDecimal minFinalAmount,
            BigDecimal maxFinalAmount,
            LocalDateTime fromTime,
            LocalDateTime toTime) {
        log.debug("action=getTransactionsWithFilters | userId={} fromTime={} toTime={}", userId, fromTime, toTime);
        if (minTotalAmount != null && maxTotalAmount != null && minTotalAmount.compareTo(maxTotalAmount) > 0) {
            throw new IllegalArgumentException("minTotalAmount cannot be greater than maxTotalAmount");
        }
        if (minFinalAmount != null && maxFinalAmount != null && minFinalAmount.compareTo(maxFinalAmount) > 0) {
            throw new IllegalArgumentException("minFinalAmount cannot be greater than maxFinalAmount");
        }
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw new IllegalArgumentException("fromTime cannot be after toTime");
        }

        List<Transaction> results = transactionRepository.findAllByTenantIdWithFilters(
                TenantContext.requireTenantId(),
                userId,
                minTotalAmount,
                maxTotalAmount,
                minFinalAmount,
                maxFinalAmount,
                fromTime,
                toTime);
        log.info("action=getTransactionsWithFilters completed | resultCount={}", results.size());
        return results;
    }

}
