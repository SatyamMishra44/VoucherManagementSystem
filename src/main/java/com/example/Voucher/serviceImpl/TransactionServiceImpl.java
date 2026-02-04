package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Transaction;
import com.example.Voucher.repository.TransactionRepository;
import com.example.Voucher.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {
    private  final TransactionRepository transactionRepository;

    //Constructor injection
    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction createTransaction(Transaction transaction) {

        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        if (transaction.getTotalAmount() <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero");
        }

        if (transaction.getFinalAmount() < 0) {
            throw new IllegalArgumentException("Final amount cannot be negative");
        }

        if (transaction.getFinalAmount() > transaction.getTotalAmount()) {
            throw new IllegalArgumentException("Final amount cannot exceed total amount");
        }

        return transactionRepository.save(transaction);
    }

    @Override
    public Optional<Transaction> getTransactionById(Long transactionId) {

        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }

        return transactionRepository.findById(transactionId);
    }

    /**
     * Fetch all transactions for a user
     */
    @Override
    public List<Transaction> getTransactionByUserId(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return transactionRepository.findByUserId(userId);
    }

}
