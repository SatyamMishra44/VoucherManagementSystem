package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Bill;
import com.example.Voucher.entity.Transaction;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.Voucher;
import com.example.Voucher.entity.VoucherRedemption;
import com.example.Voucher.repository.BillRepository;
import com.example.Voucher.repository.TransactionRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.repository.VoucherRedemptionRepository;
import com.example.Voucher.repository.VoucherRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherRedemptionServiceImplTest {

    @Mock
    private VoucherRepository voucherRepository;
    @Mock
    private VoucherRedemptionRepository voucherRedemptionRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BillRepository billRepository;

    @InjectMocks
    private VoucherRedemptionServiceImpl voucherRedemptionService;

    @Test
    void redeemVoucher_whenValidInput_shouldCreateTransactionAndRedemption() {
        User user = user(1L, "user@example.com");
        Voucher voucher = activeVoucher("SAVE10", user, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), 100.0);
        Bill bill = bill(10L, user, 200);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(voucherRepository.findByCodeAndUserIdForUpdate("SAVE10", 1L)).thenReturn(Optional.of(voucher));
        when(billRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(bill));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(voucherRedemptionRepository.save(any(VoucherRedemption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction created = voucherRedemptionService.redeemVoucher(1L, "SAVE10", 10L);

        assertNotNull(created);
        assertEquals(200, created.getTotalAmount());
        assertEquals(180, created.getFinalAmount());
        verify(transactionRepository).save(any(Transaction.class));
        verify(voucherRedemptionRepository).save(any(VoucherRedemption.class));
        verify(voucherRepository).save(voucher);
        assertEquals(true, voucher.getIsRedeemed());
        assertEquals(10L, voucher.getRedeemedBill().getId());
    }

    @Test
    void redeemVoucher_whenVoucherAlreadyRedeemed_shouldThrow() {
        User user = user(1L, "user@example.com");
        Voucher voucher = activeVoucher("SAVE10", user, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), 100.0);
        Bill bill = bill(10L, user, 200);
        voucher.markRedeemed(bill);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(voucherRepository.findByCodeAndUserIdForUpdate("SAVE10", 1L)).thenReturn(Optional.of(voucher));
        when(billRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(bill));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> voucherRedemptionService.redeemVoucher(1L, "SAVE10", 10L)
        );

        assertEquals("Voucher already redeemed", ex.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(voucherRedemptionRepository, never()).save(any(VoucherRedemption.class));
    }

    @Test
    void redeemVoucher_whenVoucherDisabled_shouldThrow() {
        User user = user(1L, "user@example.com");
        Voucher voucher = activeVoucher("SAVE10", user, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), 100.0);
        voucher.disable();
        Bill bill = bill(10L, user, 200);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(voucherRepository.findByCodeAndUserIdForUpdate("SAVE10", 1L)).thenReturn(Optional.of(voucher));
        when(billRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(bill));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> voucherRedemptionService.redeemVoucher(1L, "SAVE10", 10L)
        );

        assertEquals("voucher is disabled", ex.getMessage());
    }

    @Test
    void redeemVoucher_whenBillAmountBelowMinimum_shouldThrow() {
        User user = user(1L, "user@example.com");
        Voucher voucher = activeVoucher("SAVE10", user, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), 500.0);
        Bill bill = bill(10L, user, 200);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(voucherRepository.findByCodeAndUserIdForUpdate("SAVE10", 1L)).thenReturn(Optional.of(voucher));
        when(billRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(bill));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> voucherRedemptionService.redeemVoucher(1L, "SAVE10", 10L)
        );

        assertEquals("bill amount is below the voucher minimum", ex.getMessage());
    }

    @Test
    void redeemVoucher_whenValid_shouldPersistDiscountAppliedInHistory() {
        User user = user(1L, "user@example.com");
        Voucher voucher = activeVoucher("SAVE25", user, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), 100.0);
        Bill bill = bill(10L, user, 400);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(voucherRepository.findByCodeAndUserIdForUpdate("SAVE25", 1L)).thenReturn(Optional.of(voucher));
        when(billRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(bill));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        voucherRedemptionService.redeemVoucher(1L, "SAVE25", 10L);

        ArgumentCaptor<VoucherRedemption> captor = ArgumentCaptor.forClass(VoucherRedemption.class);
        verify(voucherRedemptionRepository).save(captor.capture());
        assertEquals(40, captor.getValue().getDiscountApplied());
    }

    @Test
    void redeemVoucher_whenBillNotOwnedByUser_shouldThrow() {
        User user = user(1L, "user@example.com");
        Voucher voucher = activeVoucher("SAVE10", user, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), 100.0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(voucherRepository.findByCodeAndUserIdForUpdate("SAVE10", 1L)).thenReturn(Optional.of(voucher));
        when(billRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> voucherRedemptionService.redeemVoucher(1L, "SAVE10", 10L)
        );

        assertEquals("Bill not found", ex.getMessage());
    }

    private static User user(Long id, String email) {
        User user = new User("Test", "User", "password", "9999999999", email, LocalDateTime.now());
        setField(user, "id", id);
        return user;
    }

    private static Bill bill(Long id, User user, Integer totalAmount) {
        Bill bill = new Bill(user, totalAmount);
        setField(bill, "id", id);
        return bill;
    }

    private static Voucher activeVoucher(
            String code,
            User owner,
            LocalDate startDate,
            LocalDate expiryDate,
            double minBillAmount
    ) {
        Voucher voucher = new Voucher(code, 10.0, minBillAmount, startDate, expiryDate, owner, owner);
        setField(voucher, "id", 99L);
        return voucher;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
