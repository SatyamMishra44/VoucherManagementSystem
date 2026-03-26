package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Bill;
import com.example.Voucher.entity.User;
import com.example.Voucher.repository.BillRepository;
import com.example.Voucher.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillServiceImplTest {

    @Mock
    private BillRepository billRepository; // create a fake version of billRepository

    private BillServiceImpl billService;

    @BeforeEach
    void setUp() { // run before every test case
        billService = new BillServiceImpl(billRepository); // injecting the mock repository
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createBill_nullBill_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> billService.createBill(null));
        assertTrue(ex.getMessage().contains("Bill cannot be null"));
        verifyNoInteractions(billRepository);
    }

    @Test
    void createBill_nullUser_throws() {
        Bill bill = mock(Bill.class);
        when(bill.getUser()).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> billService.createBill(bill));
        assertTrue(ex.getMessage().contains("Bill must be associated with a user"));
        verifyNoInteractions(billRepository);
    }

    @Test
    void createBill_nullTotalAmount_throws() {
        Bill bill = mock(Bill.class);
        when(bill.getUser()).thenReturn(buildUser());
        when(bill.getTotalAmount()).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> billService.createBill(bill));
        assertTrue(ex.getMessage().contains("Bill amount must be greater than zero"));
        verifyNoInteractions(billRepository);
    }

    @Test
    void createBill_validBill_savesAndReturns() {
        // Arrange
        User user = buildUser();
        Bill bill = new Bill(user, new BigDecimal("100.00"), null);

        // Act
        when(billRepository.save(bill)).thenReturn(bill);

        Bill saved = billService.createBill(bill);

        // Assert
        assertSame(bill, saved);
        verify(billRepository).save(bill);
    }

    @Test
    void getBillById_nullId_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> billService.getBillById(null));
        assertTrue(ex.getMessage().contains("Bill ID cannot be null"));
        verifyNoInteractions(billRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = { "0.00", "-1.00" })
    void createBill_nonPositiveTotalAmount_throws(String amount) {
        Bill bill = mock(Bill.class);
        when(bill.getUser()).thenReturn(buildUser());
        when(bill.getTotalAmount()).thenReturn(new BigDecimal(amount));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> billService.createBill(bill));
        assertTrue(ex.getMessage().contains("Bill amount must be greater than zero"));
        verifyNoInteractions(billRepository);
    }

    @Test
    void getBillById_validId_returnsBill() {
        User user = buildUser();
        Bill bill = new Bill(user, new BigDecimal("50.00"), null);
        when(billRepository.findByIdAndTenantId(10L, 1L)).thenReturn(java.util.Optional.of(bill));

        java.util.Optional<Bill> result = billService.getBillById(10L);

        assertTrue(result.isPresent());
        assertSame(bill, result.orElseThrow());
        verify(billRepository).findByIdAndTenantId(10L, 1L);
    }

    @Test
    void getBillsByUserId_nullUserId_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> billService.getBillsByUserId(null));
        assertTrue(ex.getMessage().contains("User ID cannot be null"));
        verifyNoInteractions(billRepository);
    }

    @Test
    void getBillsByUserId_validUserId_returnsList() {
        User user = buildUser();
        Bill bill = new Bill(user, new BigDecimal("120.00"), null);
        java.util.List<Bill> bills = java.util.List.of(bill);
        when(billRepository.findByUserIdAndTenantId(5L, 1L)).thenReturn(bills);

        java.util.List<Bill> result = billService.getBillsByUserId(5L);

        assertSame(bills, result);
        verify(billRepository).findByUserIdAndTenantId(5L, 1L);
    }

    @Test
    void calculateTotalAmount_billNotFound_throws() {
        when(billRepository.findByIdAndTenantId(99L, 1L)).thenReturn(java.util.Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> billService.calculateTotalAmount(99L)
        );
        assertTrue(ex.getMessage().contains("Bill not found"));
        verify(billRepository).findByIdAndTenantId(99L, 1L);
    }

    @Test
    void calculateTotalAmount_billFound_returnsAmount() {
        User user = buildUser();
        Bill bill = new Bill(user, new BigDecimal("75.50"), null);
        when(billRepository.findByIdAndTenantId(7L, 1L)).thenReturn(java.util.Optional.of(bill));

        BigDecimal result = billService.calculateTotalAmount(7L);

        assertEquals(new BigDecimal("75.50"), result);
        verify(billRepository).findByIdAndTenantId(7L, 1L);
    }

    private User buildUser() {
        User user = new User(
                "Test",
                "User",
                "hash",
                "1234567890",
                "test@example.com",
                LocalDateTime.now());
        user.setTenantId(1L);
        return user;
    }
}
