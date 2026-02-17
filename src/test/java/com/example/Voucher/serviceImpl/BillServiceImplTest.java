package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Bill;
import com.example.Voucher.entity.User;
import com.example.Voucher.repository.BillRepository;
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
class BillServiceImplTest {

    @Mock
    private BillRepository billRepository;

    @InjectMocks
    private BillServiceImpl billService;

    @Test
    void createBill_whenBillIsNull_shouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> billService.createBill(null));
        assertEquals("Bill cannot be null", ex.getMessage());
        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    void createBill_whenUserMissing_shouldThrowIllegalArgumentException() {
        Bill bill = new Bill(null, 500);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> billService.createBill(bill));

        assertEquals("Bill must be associated with a user", ex.getMessage());
        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    void createBill_whenAmountInvalid_shouldThrowIllegalArgumentException() {
        User user = user();
        Bill bill = new Bill(user, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> billService.createBill(bill));

        assertEquals("Bill amount must be greater than zero", ex.getMessage());
        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    void createBill_whenValidInput_shouldSaveBill() {
        User user = user();
        Bill bill = new Bill(user, 750);
        when(billRepository.save(bill)).thenReturn(bill);

        Bill saved = billService.createBill(bill);

        assertEquals(750, saved.getTotalAmount());
        verify(billRepository).save(bill);
    }

    private static User user() {
        return new User("Bill", "User", "pwd", "7777777777", "bill@example.com", LocalDateTime.now());
    }
}
