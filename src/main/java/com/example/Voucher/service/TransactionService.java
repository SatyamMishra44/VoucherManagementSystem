package com.example.Voucher.service;

import com.example.Voucher.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionService {

    //create a transaction
    Transaction createTransaction(Transaction transaction);

    //get transaction by its ID option will force you to handle the NullPointerException
    Optional<Transaction> getTransactionById(Long transactionId);

    List<Transaction> getTransactionByUserId(Long UserId);

    List<Transaction> getTransactionsWithFilters(
            Long userId,
            BigDecimal minTotalAmount,
            BigDecimal maxTotalAmount,
            BigDecimal minFinalAmount,
            BigDecimal maxFinalAmount,
            LocalDateTime fromTime,
            LocalDateTime toTime
    );

}
