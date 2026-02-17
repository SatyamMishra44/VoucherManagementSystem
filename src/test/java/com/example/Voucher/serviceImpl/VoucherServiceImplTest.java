package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Bill;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.Voucher;
import com.example.Voucher.repository.VoucherRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceImplTest {

    @Mock
    private VoucherRepository voucherRepository;

    @InjectMocks
    private VoucherServiceImpl voucherService;

    @Test
    void getEligibleVouchers_whenMixedData_shouldReturnOnlyEligibleVouchers() {
        User owner = user(7L, "owner@example.com");
        Voucher eligible = voucher("ELIGIBLE", owner, true, LocalDate.now().minusDays(2), LocalDate.now().plusDays(2));
        Voucher disabled = voucher("DISABLED", owner, false, LocalDate.now().minusDays(2), LocalDate.now().plusDays(2));
        Voucher expired = voucher("EXPIRED", owner, true, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1));
        Voucher redeemed = voucher("REDEEMED", owner, true, LocalDate.now().minusDays(2), LocalDate.now().plusDays(2));
        redeemed.markRedeemed(new Bill(owner, 500));

        when(voucherRepository.findByUserId(7L)).thenReturn(List.of(eligible, disabled, expired, redeemed));

        List<Voucher> result = voucherService.getEligibleVouchers(7L);

        assertEquals(1, result.size());
        assertEquals("ELIGIBLE", result.getFirst().getCode());
    }

    @Test
    void validateVoucher_whenCodeNotFound_shouldThrow() {
        when(voucherRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> voucherService.validateVoucher("NOPE"));

        assertEquals("Invalid voucher code", ex.getMessage());
    }

    @Test
    void validateVoucher_whenVoucherDisabled_shouldThrow() {
        User owner = user(11L, "owner@example.com");
        Voucher voucher = voucher("SAVE10", owner, false, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        when(voucherRepository.findByCode("SAVE10")).thenReturn(Optional.of(voucher));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> voucherService.validateVoucher("SAVE10"));

        assertEquals("voucher is disabled", ex.getMessage());
    }

    @Test
    void validateVoucher_whenAlreadyRedeemed_shouldThrow() {
        User owner = user(11L, "owner@example.com");
        Voucher voucher = voucher("LIMIT", owner, true, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        voucher.markRedeemed(new Bill(owner, 900));
        when(voucherRepository.findByCode("LIMIT")).thenReturn(Optional.of(voucher));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> voucherService.validateVoucher("LIMIT"));

        assertEquals("Voucher already redeemed", ex.getMessage());
    }

    private static User user(Long id, String email) {
        User user = new User("Creator", "Admin", "pwd", "8888888888", email, LocalDateTime.now());
        setField(user, "id", id);
        return user;
    }

    private static Voucher voucher(
            String code,
            User owner,
            boolean enabled,
            LocalDate startDate,
            LocalDate expiryDate
    ) {
        Voucher voucher = new Voucher(code, 10.0, 100.0, startDate, expiryDate, owner, owner);
        setField(voucher, "id", Math.abs(code.hashCode()) + 1L);
        if (!enabled) {
            voucher.disable();
        }
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
