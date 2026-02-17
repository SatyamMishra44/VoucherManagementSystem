package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Transaction;
import com.example.Voucher.entity.User;
import com.example.Voucher.repository.TransactionRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    void createTransaction_whenTransactionIsNull_shouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(null)
        );
        assertEquals("Transaction cannot be null", ex.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_whenTotalAmountNotPositive_shouldThrowIllegalArgumentException() {
        Transaction transaction = new Transaction(user(), 0, 0);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(transaction)
        );

        assertEquals("Total amount must be greater than zero", ex.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_whenFinalAmountNegative_shouldThrowIllegalArgumentException() {
        Transaction transaction = new Transaction(user(), 100, -1);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(transaction)
        );

        assertEquals("Final amount cannot be negative", ex.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_whenFinalAmountExceedsTotal_shouldThrowIllegalArgumentException() {
        Transaction transaction = new Transaction(user(), 100, 120);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(transaction)
        );

        assertEquals("Final amount cannot exceed total amount", ex.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_whenValidInput_shouldSaveTransaction() {
        Transaction transaction = new Transaction(user(), 200, 150);
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        Transaction saved = transactionService.createTransaction(transaction);

        assertEquals(200, saved.getTotalAmount());
        assertEquals(150, saved.getFinalAmount());
        verify(transactionRepository).save(transaction);
    }

    private static User user() {
        return new User("Txn", "User", "pwd", "6666666666", "txn@example.com", LocalDateTime.now());
    }
}
