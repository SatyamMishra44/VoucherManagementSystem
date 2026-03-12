package com.example.Voucher.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Voucher.entity.Bill;
import com.example.Voucher.entity.User;
import com.example.Voucher.repository.BillRepository;
import com.example.Voucher.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillServiceImplTest {

    @Mock
    private BillRepository billRepository;

    private BillServiceImpl billService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
        billService = new BillServiceImpl(billRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createBill_whenValidBill_savesAndReturnsBill() {
        User user = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        setField(user, "tenantId", 1L);
        Bill bill = new Bill(user, BigDecimal.valueOf(500));

        when(billRepository.save(bill)).thenReturn(bill);

        Bill saved = billService.createBill(bill);

        assertEquals(BigDecimal.valueOf(500).setScale(2), saved.getTotalAmount());
        verify(billRepository).save(bill);
    }

    @Test
    void createBill_whenBillIsNull_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> billService.createBill(null));

        assertEquals("Bill cannot be null", ex.getMessage());
    }

    @Test
    void createBill_whenUserIsNull_throwsIllegalArgumentException() {
        Bill bill = new Bill(null, BigDecimal.valueOf(500));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> billService.createBill(bill));

        assertEquals("Bill must be associated with a user", ex.getMessage());
    }

    @Test
    void createBill_whenAmountIsZero_throwsIllegalArgumentException() {
        User user = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        setField(user, "tenantId", 1L);
        Bill bill = new Bill(user, BigDecimal.ZERO);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> billService.createBill(bill));

        assertEquals("Bill amount must be greater than zero", ex.getMessage());
    }

    @Test
    void getBillById_whenIdIsNull_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> billService.getBillById(null));

        assertEquals("Bill ID cannot be null", ex.getMessage());
    }

    @Test
    void calculateTotalAmount_whenBillExists_returnsAmount() {
        User user = new User("Sam", "K", "hash", "9876543210", "sam@example.com", LocalDateTime.now());
        setField(user, "tenantId", 1L);
        Bill bill = new Bill(user, BigDecimal.valueOf(250));

        when(billRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(bill));

        BigDecimal total = billService.calculateTotalAmount(1L);

        assertEquals(BigDecimal.valueOf(250).setScale(2), total);
    }

    @Test
    void calculateTotalAmount_whenBillMissing_throwsRuntimeException() {
        when(billRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> billService.calculateTotalAmount(99L));

        assertEquals("Bill not found", ex.getMessage());
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
