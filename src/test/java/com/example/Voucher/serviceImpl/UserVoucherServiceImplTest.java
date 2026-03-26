package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Bill;
import com.example.Voucher.entity.RedemptionHistory;
import com.example.Voucher.entity.Transaction;
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
import com.example.Voucher.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

        private UserVoucherServiceImpl userVoucherService;

        @BeforeEach
        void setUp() {
                userVoucherService = new UserVoucherServiceImpl(
                                userRepository,
                                voucherTemplateRepository,
                                userVoucherRepository,
                                redemptionHistoryRepository,
                                billRepository,
                                transactionRepository);
                TenantContext.setTenantId(1L);
        }

        @AfterEach
        void tearDown() {
                TenantContext.clear();
        }

        @Test
        void purchaseVoucher_nullUserId_throws() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.purchaseVoucher(null, "CODE", 1, null));
                assertTrue(ex.getMessage().contains("User id is required"));
                verifyNoInteractions(userRepository, voucherTemplateRepository, userVoucherRepository);
        }

        @Test
        void purchaseVoucher_blankVoucherCode_throws() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.purchaseVoucher(1L, " ", 1, null));
                assertTrue(ex.getMessage().contains("Voucher code is required"));
                verifyNoInteractions(userRepository, voucherTemplateRepository, userVoucherRepository);
        }

        @Test
        void purchaseVoucher_nullQuantity_throws() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.purchaseVoucher(1L, "CODE", null, null));
                assertTrue(ex.getMessage().contains("Quantity must be greater than zero"));
                verifyNoInteractions(userRepository, voucherTemplateRepository, userVoucherRepository);
        }

        @Test
        void purchaseVoucher_nonPositiveQuantity_throws() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.purchaseVoucher(1L, "CODE", 0, null));
                assertTrue(ex.getMessage().contains("Quantity must be greater than zero"));
                verifyNoInteractions(userRepository, voucherTemplateRepository, userVoucherRepository);
        }

    @Test
    void purchaseVoucher_userNotFound_throws() {
        when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userVoucherService.purchaseVoucher(1L, "CODE", 1, null)
        );
        assertTrue(ex.getMessage().contains("User not found"));
    }

        @Test
        void purchaseVoucher_templateNotFound_throws() {
                User user = buildUser();
                when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
                when(voucherTemplateRepository.findByCodeAndTenantId("CODE", 1L)).thenReturn(Optional.empty());

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.purchaseVoucher(1L, "CODE", 1, null));
                assertTrue(ex.getMessage().contains("Voucher template not found"));
        }

        @Test
        void purchaseVoucher_disabledTemplate_throws() {
                User user = buildUser();
                VoucherTemplate template = validTemplate();
                template.disable();
                when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
                when(voucherTemplateRepository.findByCodeAndTenantId("CODE", 1L)).thenReturn(Optional.of(template));

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.purchaseVoucher(1L, "CODE", 1, null));
                assertTrue(ex.getMessage().contains("Voucher template is disabled"));
        }

        @Test
        void purchaseVoucher_templateNotValidToday_throws() {
                User user = buildUser();
                VoucherTemplate template = new VoucherTemplate(
                                "CODE",
                                new BigDecimal("20.00"),
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusDays(10));
                template.setTenantId(1L);
                when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
                when(voucherTemplateRepository.findByCodeAndTenantId("CODE", 1L)).thenReturn(Optional.of(template));

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.purchaseVoucher(1L, "CODE", 1, null));
                assertTrue(ex.getMessage().contains("Voucher template is not valid on this date"));
        }

        @Test
        void purchaseVoucher_valid_savesVoucher() {
                User user = buildUser();
                VoucherTemplate template = validTemplate();
                when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
                when(voucherTemplateRepository.findByCodeAndTenantId("CODE", 1L)).thenReturn(Optional.of(template));
                when(userVoucherRepository.save(any(UserVoucher.class))).thenAnswer(inv -> inv.getArgument(0));

                UserVoucher saved = userVoucherService.purchaseVoucher(1L, "CODE", 3, null);

                assertEquals(new BigDecimal("60.00"), saved.getTotalPurchasedAmount());
                assertEquals(new BigDecimal("60.00"), saved.getRemainingBalance());
                verify(userVoucherRepository).save(any(UserVoucher.class));
        }

        @Test
        void redeemVoucher_nullUserId_throws() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.redeemVoucher(null, 1L, 1L, null));
                assertTrue(ex.getMessage().contains("User id is required"));
        }

        @Test
        void redeemVoucher_nullUserVoucherId_throws() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.redeemVoucher(1L, null, 1L, null));
                assertTrue(ex.getMessage().contains("User voucher id is required"));
        }

        @Test
        void redeemVoucher_nullBillId_throws() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.redeemVoucher(1L, 1L, null, null));
                assertTrue(ex.getMessage().contains("Bill id is required"));
        }

    @Test
    void redeemVoucher_userNotFound_throws() {
        when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userVoucherService.redeemVoucher(1L, 2L, 3L, null)
        );
        assertTrue(ex.getMessage().contains("User not found"));
    }

        @Test
        void redeemVoucher_userVoucherNotFound_throws() {
                User user = buildUser();
                when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
                when(userVoucherRepository.findByIdAndUserIdForUpdate(2L, 1L, 1L)).thenReturn(Optional.empty());

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.redeemVoucher(1L, 2L, 3L, null));
                assertTrue(ex.getMessage().contains("User voucher not found"));
        }

        @Test
        void redeemVoucher_inactiveVoucher_throws() {
                User user = buildUser();
                UserVoucher userVoucher = mock(UserVoucher.class);
                when(userVoucher.isActive()).thenReturn(false);
                when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
                when(userVoucherRepository.findByIdAndUserIdForUpdate(2L, 1L, 1L)).thenReturn(Optional.of(userVoucher));

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.redeemVoucher(1L, 2L, 3L, null));
                assertTrue(ex.getMessage().contains("User voucher is inactive"));
        }

        @Test
        void redeemVoucher_templateDisabled_throws() {
                User user = buildUser();
                VoucherTemplate template = validTemplate();
                template.disable();
                UserVoucher userVoucher = new UserVoucher(template, user, 1, new BigDecimal("20.00"),
                                new BigDecimal("20.00"), null);
                when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
                when(userVoucherRepository.findByIdAndUserIdForUpdate(2L, 1L, 1L)).thenReturn(Optional.of(userVoucher));

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.redeemVoucher(1L, 2L, 3L, null));
                assertTrue(ex.getMessage().contains("Voucher template is disabled"));
        }

        @Test
        void redeemVoucher_noRemainingBalance_throws() {
                User user = buildUser();
                VoucherTemplate template = validTemplate();
                UserVoucher userVoucher = new UserVoucher(template, user, 1, new BigDecimal("0.00"),
                                new BigDecimal("0.00"), null);
                when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
                when(userVoucherRepository.findByIdAndUserIdForUpdate(2L, 1L, 1L)).thenReturn(Optional.of(userVoucher));

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.redeemVoucher(1L, 2L, 3L, null));
                assertTrue(ex.getMessage().contains("User voucher has no remaining balance"));
        }

        @Test
        void redeemVoucher_billNotFound_throws() {
                User user = buildUser();
                VoucherTemplate template = validTemplate();
                UserVoucher userVoucher = new UserVoucher(template, user, 1, new BigDecimal("20.00"),
                                new BigDecimal("20.00"), null);
                when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
                when(userVoucherRepository.findByIdAndUserIdForUpdate(2L, 1L, 1L)).thenReturn(Optional.of(userVoucher));
                when(billRepository.findByIdAndUserIdAndTenantId(3L, 1L, 1L)).thenReturn(Optional.empty());

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.redeemVoucher(1L, 2L, 3L, null));
                assertTrue(ex.getMessage().contains("Bill not found"));
        }

        @Test
        void redeemVoucher_valid_redeemsAndReturnsResult() {
                User user = buildUser();
                VoucherTemplate template = validTemplate();
                UserVoucher userVoucher = new UserVoucher(template, user, 3, new BigDecimal("60.00"),
                                new BigDecimal("60.00"), null);
                Bill bill = new Bill(user, new BigDecimal("50.00"), null);

                when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
                when(userVoucherRepository.findByIdAndUserIdForUpdate(2L, 1L, 1L)).thenReturn(Optional.of(userVoucher));
                when(billRepository.findByIdAndUserIdAndTenantId(3L, 1L, 1L)).thenReturn(Optional.of(bill));
                when(userVoucherRepository.save(userVoucher)).thenReturn(userVoucher);
                when(redemptionHistoryRepository.save(any(RedemptionHistory.class)))
                                .thenAnswer(inv -> inv.getArgument(0));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

                RedemptionResult result = userVoucherService.redeemVoucher(1L, 2L, 3L, null);

                assertEquals(new BigDecimal("50.00"), result.getBillAmount());
                assertEquals(new BigDecimal("50.00"), result.getRedeemedAmount());
                assertEquals(new BigDecimal("0.00"), result.getPayableAmount());
                assertEquals(bill.getId(), result.getBillId());
                assertSame(userVoucher, result.getUserVoucher());
                assertEquals(new BigDecimal("10.00"), userVoucher.getRemainingBalance());
                assertTrue(userVoucher.isActive());

                ArgumentCaptor<RedemptionHistory> historyCaptor = ArgumentCaptor.forClass(RedemptionHistory.class);
                verify(redemptionHistoryRepository).save(historyCaptor.capture());
                RedemptionHistory history = historyCaptor.getValue();
                assertEquals(new BigDecimal("50.00"), history.getRedeemedAmount());
                assertEquals(new BigDecimal("10.00"), history.getRemainingBalanceAfter());

                ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
                verify(transactionRepository).save(transactionCaptor.capture());
                Transaction transaction = transactionCaptor.getValue();
                assertEquals(new BigDecimal("50.00"), transaction.getTotalAmount());
                assertEquals(new BigDecimal("0.00"), transaction.getFinalAmount());
        }

        @Test
        void getUserVouchers_nullUserId_throws() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.getUserVouchers(null));
                assertTrue(ex.getMessage().contains("User id is required"));
                verifyNoInteractions(userVoucherRepository);
        }

        @Test
        void getUserVouchers_validUserId_returnsList() {
                List<UserVoucher> vouchers = List.of(mock(UserVoucher.class));
                when(userVoucherRepository.findByUserIdAndTenantIdOrderByPurchasedAtDesc(1L, 1L)).thenReturn(vouchers);

                List<UserVoucher> result = userVoucherService.getUserVouchers(1L);

                assertSame(vouchers, result);
                verify(userVoucherRepository).findByUserIdAndTenantIdOrderByPurchasedAtDesc(1L, 1L);
        }

        @Test
        void getUserRedemptionHistory_nullUserId_throws() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.getUserRedemptionHistory(null));
                assertTrue(ex.getMessage().contains("User id is required"));
                verifyNoInteractions(redemptionHistoryRepository);
        }

        @Test
        void getUserRedemptionHistory_validUserId_returnsList() {
                List<RedemptionHistory> history = List.of(mock(RedemptionHistory.class));
                when(redemptionHistoryRepository
                                .findByUserVoucherUserIdAndTenantIdOrderByRedeemedAtDesc(1L, 1L))
                                .thenReturn(history);

                List<RedemptionHistory> result = userVoucherService.getUserRedemptionHistory(1L);

                assertSame(history, result);
                verify(redemptionHistoryRepository)
                                .findByUserVoucherUserIdAndTenantIdOrderByRedeemedAtDesc(1L, 1L);
        }

        @Test
        void getAdminFilteredVouchers_minGreaterThanMax_throws() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.getAdminFilteredVouchers(
                                                null,
                                                new BigDecimal("100.00"),
                                                new BigDecimal("50.00"),
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null));
                assertTrue(ex.getMessage().contains("minVoucherAmount cannot be greater than maxVoucherAmount"));
                verifyNoInteractions(userVoucherRepository);
        }

        @Test
        void getAdminFilteredVouchers_issuedFromAfterIssuedTo_throws() {
                LocalDate issuedFrom = LocalDate.now();
                LocalDate issuedTo = issuedFrom.minusDays(1);

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.getAdminFilteredVouchers(
                                                null,
                                                null,
                                                null,
                                                null,
                                                issuedFrom,
                                                issuedTo,
                                                null,
                                                null,
                                                null));
                assertTrue(ex.getMessage().contains("issuedFrom cannot be after issuedTo"));
                verifyNoInteractions(userVoucherRepository);
        }

        @Test
        void getAdminFilteredVouchers_expiryFromAfterExpiryTo_throws() {
                LocalDate expiryFrom = LocalDate.now();
                LocalDate expiryTo = expiryFrom.minusDays(1);

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.getAdminFilteredVouchers(
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                expiryFrom,
                                                expiryTo,
                                                null));
                assertTrue(ex.getMessage().contains("expiryFrom cannot be after expiryTo"));
                verifyNoInteractions(userVoucherRepository);
        }

        @Test
        void getAdminFilteredVouchers_invalidRedemptionState_throws() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.getAdminFilteredVouchers(
                                                null,
                                                null,
                                                null,
                                                "bad",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null));
                assertTrue(ex.getMessage().contains("Invalid redemptionState"));
        }

        @Test
        void getAdminFilteredVouchers_invalidStatus_throws() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userVoucherService.getAdminFilteredVouchers(
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                "bad"));
                assertTrue(ex.getMessage().contains("Invalid status"));
        }

        @Test
        void getAdminFilteredVouchers_valid_normalizesInputs() {
                List<UserVoucher> vouchers = List.of(mock(UserVoucher.class));
                LocalDate issuedFrom = LocalDate.of(2024, 1, 1);
                LocalDate issuedTo = LocalDate.of(2024, 1, 31);
                LocalDate expiryFrom = LocalDate.of(2024, 2, 1);
                LocalDate expiryTo = LocalDate.of(2024, 2, 28);

                when(userVoucherRepository.findAllByTenantIdWithAdminFilters(
                                1L,
                                10L,
                                new BigDecimal("10.00"),
                                new BigDecimal("100.00"),
                                "PARTIALLY_REDEEMED",
                                issuedFrom.atStartOfDay(),
                                issuedTo.atTime(23, 59, 59),
                                expiryFrom,
                                expiryTo,
                                UserVoucherStatus.ACTIVE)).thenReturn(vouchers);

                List<UserVoucher> result = userVoucherService.getAdminFilteredVouchers(
                                10L,
                                new BigDecimal("10.00"),
                                new BigDecimal("100.00"),
                                " partially_redeemed ",
                                issuedFrom,
                                issuedTo,
                                expiryFrom,
                                expiryTo,
                                " active ");

                assertSame(vouchers, result);
                verify(userVoucherRepository).findAllByTenantIdWithAdminFilters(
                                1L,
                                10L,
                                new BigDecimal("10.00"),
                                new BigDecimal("100.00"),
                                "PARTIALLY_REDEEMED",
                                issuedFrom.atStartOfDay(),
                                issuedTo.atTime(23, 59, 59),
                                expiryFrom,
                                expiryTo,
                                UserVoucherStatus.ACTIVE);
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

        private VoucherTemplate validTemplate() {
                VoucherTemplate template = new VoucherTemplate(
                                "CODE",
                                new BigDecimal("20.00"),
                                LocalDate.now().minusDays(1),
                                LocalDate.now().plusDays(10));
                template.setTenantId(1L);
                return template;
        }
}
