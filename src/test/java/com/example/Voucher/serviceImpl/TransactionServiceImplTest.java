package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Bill;
import com.example.Voucher.entity.Transaction;
import com.example.Voucher.entity.User;
import com.example.Voucher.repository.TransactionRepository;
import com.example.Voucher.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(transactionRepository);
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createTransaction_nullTransaction_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(null)
        );
        assertTrue(ex.getMessage().contains("Transaction cannot be null"));
        verifyNoInteractions(transactionRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1.00"})
    void createTransaction_totalAmountNotPositive_throws(String amount) {
        Transaction transaction = mock(Transaction.class);
        when(transaction.getTotalAmount()).thenReturn(new BigDecimal(amount));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(transaction)
        );
        assertTrue(ex.getMessage().contains("Total amount must be greater than zero"));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void createTransaction_finalAmountNegative_throws() {
        Transaction transaction = mock(Transaction.class);
        when(transaction.getTotalAmount()).thenReturn(new BigDecimal("10.00"));
        when(transaction.getFinalAmount()).thenReturn(new BigDecimal("-0.01"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(transaction)
        );
        assertTrue(ex.getMessage().contains("Final amount cannot be negative"));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void createTransaction_finalAmountExceedsTotal_throws() {
        Transaction transaction = mock(Transaction.class);
        when(transaction.getTotalAmount()).thenReturn(new BigDecimal("10.00"));
        when(transaction.getFinalAmount()).thenReturn(new BigDecimal("11.00"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(transaction)
        );
        assertTrue(ex.getMessage().contains("Final amount cannot exceed total amount"));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void createTransaction_validTransaction_savesAndReturns() {
        Transaction transaction = buildTransaction();
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        Transaction saved = transactionService.createTransaction(transaction);

        assertSame(transaction, saved);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void getTransactionById_nullId_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.getTransactionById(null)
        );
        assertTrue(ex.getMessage().contains("Transaction ID cannot be null"));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void getTransactionById_matchingTenant_returnsTransaction() {
        Transaction transaction = mock(Transaction.class);
        when(transaction.getTenantId()).thenReturn(1L);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));

        Optional<Transaction> result = transactionService.getTransactionById(10L);

        assertTrue(result.isPresent());
        assertSame(transaction, result.orElseThrow());
        verify(transactionRepository).findById(10L);
    }

    @Test
    void getTransactionById_mismatchedTenant_returnsEmpty() {
        Transaction transaction = mock(Transaction.class);
        when(transaction.getTenantId()).thenReturn(2L);
        when(transactionRepository.findById(11L)).thenReturn(Optional.of(transaction));

        Optional<Transaction> result = transactionService.getTransactionById(11L);

        assertFalse(result.isPresent());
        verify(transactionRepository).findById(11L);
    }

    @Test
    void getTransactionByUserId_nullUserId_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.getTransactionByUserId(null)
        );
        assertTrue(ex.getMessage().contains("User ID cannot be null"));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void getTransactionByUserId_validUserId_returnsList() {
        Transaction transaction = buildTransaction();
        List<Transaction> transactions = List.of(transaction);
        when(transactionRepository.findByUserIdAndTenantId(5L, 1L)).thenReturn(transactions);

        List<Transaction> result = transactionService.getTransactionByUserId(5L);

        assertSame(transactions, result);
        verify(transactionRepository).findByUserIdAndTenantId(5L, 1L);
    }

    @Test
    void getTransactionsWithFilters_minTotalGreaterThanMaxTotal_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.getTransactionsWithFilters(
                        null,
                        new BigDecimal("10.00"),
                        new BigDecimal("5.00"),
                        null,
                        null,
                        null,
                        null
                )
        );
        assertTrue(ex.getMessage().contains("minTotalAmount cannot be greater than maxTotalAmount"));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void getTransactionsWithFilters_minFinalGreaterThanMaxFinal_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.getTransactionsWithFilters(
                        null,
                        null,
                        null,
                        new BigDecimal("10.00"),
                        new BigDecimal("5.00"),
                        null,
                        null
                )
        );
        assertTrue(ex.getMessage().contains("minFinalAmount cannot be greater than maxFinalAmount"));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void getTransactionsWithFilters_fromAfterTo_throws() {
        LocalDateTime fromTime = LocalDateTime.now();
        LocalDateTime toTime = fromTime.minusDays(1);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.getTransactionsWithFilters(
                        null,
                        null,
                        null,
                        null,
                        null,
                        fromTime,
                        toTime
                )
        );
        assertTrue(ex.getMessage().contains("fromTime cannot be after toTime"));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void getTransactionsWithFilters_validFilters_returnsList() {
        Transaction transaction = buildTransaction();
        List<Transaction> transactions = List.of(transaction);
        LocalDateTime fromTime = LocalDateTime.now().minusDays(7);
        LocalDateTime toTime = LocalDateTime.now();
        when(transactionRepository.findAllByTenantIdWithFilters(
                1L,
                2L,
                new BigDecimal("10.00"),
                new BigDecimal("100.00"),
                new BigDecimal("5.00"),
                new BigDecimal("90.00"),
                fromTime,
                toTime
        )).thenReturn(transactions);

        List<Transaction> result = transactionService.getTransactionsWithFilters(
                2L,
                new BigDecimal("10.00"),
                new BigDecimal("100.00"),
                new BigDecimal("5.00"),
                new BigDecimal("90.00"),
                fromTime,
                toTime
        );

        assertSame(transactions, result);
        verify(transactionRepository).findAllByTenantIdWithFilters(
                1L,
                2L,
                new BigDecimal("10.00"),
                new BigDecimal("100.00"),
                new BigDecimal("5.00"),
                new BigDecimal("90.00"),
                fromTime,
                toTime
        );
    }

    private Transaction buildTransaction() {
        User user = buildUser();
        Bill bill = new Bill(user, new BigDecimal("100.00"));
        return new Transaction(user, bill, new BigDecimal("100.00"), new BigDecimal("80.00"));
    }

    private User buildUser() {
        User user = new User(
                "Test",
                "User",
                "hash",
                "1234567890",
                "test@example.com",
                LocalDateTime.now()
        );
        user.setTenantId(1L);
        return user;
    }
}
