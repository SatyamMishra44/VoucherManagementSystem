package com.example.Voucher.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Voucher.entity.Bill;
import com.example.Voucher.entity.RedemptionHistory;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.UserVoucher;
import com.example.Voucher.entity.UserVoucherStatus;
import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.repository.BillRepository;
import com.example.Voucher.repository.RedemptionHistoryRepository;
import com.example.Voucher.repository.TransactionRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.repository.UserVoucherRepository;
import com.example.Voucher.repository.VoucherTemplateRepository;
import com.example.Voucher.service.RedemptionResult;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserVoucherServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private VoucherTemplateRepository voucherTemplateRepository;
    @Mock
    private UserVoucherRepository userVoucherRepository;
    @Mock
    private RedemptionHistoryRepository redemptionHistoryRepository;
    @Mock
    private BillRepository billRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private UserVoucherServiceImpl userVoucherService;

    private User user;
    private VoucherTemplate activeTemplate;

    @BeforeEach
    void setUp() {
        user = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        setField(user, "id", 10L);

        activeTemplate = new VoucherTemplate(
                "SAVE100",
                new BigDecimal("50.00"),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(10)
        );
    }

    @Test
    void purchaseVoucher_whenValid_savesVoucherWithCalculatedTotal() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(voucherTemplateRepository.findByCode("SAVE100")).thenReturn(Optional.of(activeTemplate));
        when(userVoucherRepository.save(any(UserVoucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserVoucher result = userVoucherService.purchaseVoucher(10L, "SAVE100", 3);

        assertNotNull(result);
        assertEquals(new BigDecimal("150.00"), result.getTotalPurchasedAmount());
        assertEquals(new BigDecimal("150.00"), result.getRemainingBalance());
        assertEquals(UserVoucherStatus.ACTIVE, result.getStatus());
        verify(userVoucherRepository).save(any(UserVoucher.class));
    }

    @Test
    void purchaseVoucher_whenTemplateDisabled_throwsIllegalArgumentException() {
        activeTemplate.disable();

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(voucherTemplateRepository.findByCode("SAVE100")).thenReturn(Optional.of(activeTemplate));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userVoucherService.purchaseVoucher(10L, "SAVE100", 1));

        assertEquals("Voucher template is disabled", ex.getMessage());
        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    void purchaseVoucher_whenQuantityInvalid_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userVoucherService.purchaseVoucher(10L, "SAVE100", 0));

        assertEquals("Quantity must be greater than zero", ex.getMessage());
    }

    @Test
    void redeemVoucher_whenBillProvided_createsRedemptionAndTransaction() {
        UserVoucher userVoucher = new UserVoucher(activeTemplate, user, 2,
                new BigDecimal("100.00"), new BigDecimal("100.00"));
        setField(userVoucher, "id", 99L);

        Bill bill = new Bill(user, new BigDecimal("80.00"));
        setField(bill, "id", 55L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userVoucherRepository.findByIdAndUserIdForUpdate(99L, 10L)).thenReturn(Optional.of(userVoucher));
        when(billRepository.findByIdAndUserId(55L, 10L)).thenReturn(Optional.of(bill));
        when(userVoucherRepository.save(any(UserVoucher.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(redemptionHistoryRepository.save(any(RedemptionHistory.class))).thenAnswer(invocation -> {
            RedemptionHistory history = invocation.getArgument(0);
            setField(history, "id", 501L);
            return history;
        });

        RedemptionResult result = userVoucherService.redeemVoucher(10L, 99L, null, 55L);

        assertEquals(Long.valueOf(501L), result.getRedemptionId());
        assertEquals(new BigDecimal("80.00"), result.getRedeemedAmount());
        assertEquals(new BigDecimal("0.00"), result.getPayableAmount());
        assertEquals(new BigDecimal("20.00"), result.getUserVoucher().getRemainingBalance());
        verify(transactionRepository).save(any());
    }

    @Test
    void redeemVoucher_whenVoucherBelongsToAnotherUser_throwsIllegalArgumentException() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userVoucherRepository.findByIdAndUserIdForUpdate(999L, 10L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userVoucherService.redeemVoucher(10L, 999L, new BigDecimal("20"), null));

        assertEquals("User voucher not found", ex.getMessage());
        verify(redemptionHistoryRepository, never()).save(any());
    }

    @Test
    void redeemVoucher_whenNoBillAndInvalidAmount_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userVoucherService.redeemVoucher(10L, 99L, BigDecimal.ZERO, null));

        assertEquals("Bill amount must be greater than zero when bill id is not provided", ex.getMessage());
    }

    @Test
    void redeemVoucher_whenBillNotOwnedByUser_throwsIllegalArgumentException() {
        UserVoucher userVoucher = new UserVoucher(activeTemplate, user, 1,
                new BigDecimal("50.00"), new BigDecimal("50.00"));
        setField(userVoucher, "id", 99L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userVoucherRepository.findByIdAndUserIdForUpdate(99L, 10L)).thenReturn(Optional.of(userVoucher));
        when(billRepository.findByIdAndUserId(77L, 10L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userVoucherService.redeemVoucher(10L, 99L, null, 77L));

        assertEquals("Bill not found", ex.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void redeemVoucher_whenRemainingBalanceExhausted_marksVoucherInactive() {
        UserVoucher userVoucher = new UserVoucher(activeTemplate, user, 1,
                new BigDecimal("50.00"), new BigDecimal("50.00"));
        setField(userVoucher, "id", 99L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userVoucherRepository.findByIdAndUserIdForUpdate(99L, 10L)).thenReturn(Optional.of(userVoucher));
        when(userVoucherRepository.save(any(UserVoucher.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(redemptionHistoryRepository.save(any(RedemptionHistory.class))).thenAnswer(invocation -> {
            RedemptionHistory history = invocation.getArgument(0);
            setField(history, "id", 777L);
            return history;
        });

        RedemptionResult result = userVoucherService.redeemVoucher(10L, 99L, new BigDecimal("80.00"), null);

        assertEquals(new BigDecimal("50.00"), result.getRedeemedAmount());
        assertEquals(new BigDecimal("30.00"), result.getPayableAmount());
        assertEquals(UserVoucherStatus.INACTIVE, result.getUserVoucher().getStatus());
        assertEquals(new BigDecimal("0.00"), result.getUserVoucher().getRemainingBalance());
        verify(transactionRepository, never()).save(any());
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
