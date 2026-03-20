package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.TenantVoucherDistribution;
import com.example.Voucher.entity.TenantVoucherInventory;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.UserVoucher;
import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.repository.TenantVoucherDistributionRepository;
import com.example.Voucher.repository.TenantVoucherInventoryRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.repository.UserVoucherRepository;
import com.example.Voucher.repository.VoucherTemplateRepository;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantConstants;
import com.example.Voucher.tenant.TenantRepository;
import com.example.Voucher.tenant.TenantType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantVoucherAdministrationServiceImplTest {

    @Mock
    private TenantVoucherInventoryRepository tenantVoucherInventoryRepository;

    @Mock
    private TenantVoucherDistributionRepository tenantVoucherDistributionRepository;

    @Mock
    private VoucherTemplateRepository voucherTemplateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserVoucherRepository userVoucherRepository;

    @Mock
    private TenantRepository tenantRepository;

    private TenantVoucherAdministrationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TenantVoucherAdministrationServiceImpl(
                tenantVoucherInventoryRepository,
                tenantVoucherDistributionRepository,
                voucherTemplateRepository,
                userRepository,
                userVoucherRepository,
                tenantRepository
        );
    }

    @Test
    void purchaseForTenant_blankCode_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.purchaseForTenant(" ", 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Voucher code is required"));
        verifyNoInteractions(tenantRepository, userRepository, voucherTemplateRepository, tenantVoucherInventoryRepository);
    }

    @Test
    void purchaseForTenant_invalidQuantity_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.purchaseForTenant("CODE", 0, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Quantity must be greater than zero"));
    }

    @Test
    void purchaseForTenant_tenantNotFound_throws() {
        when(tenantRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.purchaseForTenant("CODE", 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Tenant not found"));
    }

    @Test
    void purchaseForTenant_notOrganization_throws() {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getTenantType()).thenReturn(TenantType.SYSTEM_INDIVIDUAL);
        when(tenantRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(tenant));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.purchaseForTenant("CODE", 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Operation allowed only for organization tenants"));
    }

    @Test
    void purchaseForTenant_actorNotFound_throws() {
        stubOrgTenant(1L);
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.purchaseForTenant("CODE", 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Tenant admin user not found"));
    }

    @Test
    void purchaseForTenant_platformTenantNotFound_throws() {
        stubOrgTenant(1L);
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(buildUser(1L)));
        when(tenantRepository.findByTenantCodeAndActiveTrue(TenantConstants.SYSTEM_INDIVIDUAL_CODE))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.purchaseForTenant("CODE", 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Platform tenant not found"));
    }

    @Test
    void purchaseForTenant_templateNotFound_throws() {
        stubOrgTenant(1L);
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(buildUser(1L)));
        Tenant platformTenant = mockTenantWithId(1L);
        when(tenantRepository.findByTenantCodeAndActiveTrue(TenantConstants.SYSTEM_INDIVIDUAL_CODE))
                .thenReturn(Optional.of(platformTenant));
        when(voucherTemplateRepository.findByCodeAndTenantId("CODE", 1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.purchaseForTenant("CODE", 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Voucher template not found"));
    }

    @Test
    void purchaseForTenant_templateDisabled_throws() {
        stubOrgTenant(1L);
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(buildUser(1L)));
        Tenant platformTenant = mockTenantWithId(1L);
        when(tenantRepository.findByTenantCodeAndActiveTrue(TenantConstants.SYSTEM_INDIVIDUAL_CODE))
                .thenReturn(Optional.of(platformTenant));
        VoucherTemplate template = validTemplate(1L);
        template.disable();
        when(voucherTemplateRepository.findByCodeAndTenantId("CODE", 1L))
                .thenReturn(Optional.of(template));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.purchaseForTenant("CODE", 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Voucher template is disabled"));
    }

    @Test
    void purchaseForTenant_templateNotValid_throws() {
        stubOrgTenant(1L);
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(buildUser(1L)));
        Tenant platformTenant = mockTenantWithId(1L);
        when(tenantRepository.findByTenantCodeAndActiveTrue(TenantConstants.SYSTEM_INDIVIDUAL_CODE))
                .thenReturn(Optional.of(platformTenant));
        VoucherTemplate template = new VoucherTemplate(
                "CODE",
                new BigDecimal("10.00"),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(10)
        );
        template.setTenantId(1L);
        when(voucherTemplateRepository.findByCodeAndTenantId("CODE", 1L))
                .thenReturn(Optional.of(template));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.purchaseForTenant("CODE", 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Voucher template is not valid on this date"));
    }

    @Test
    void purchaseForTenant_actorTenantMismatch_throws() {
        stubOrgTenant(1L);
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(buildUser(2L)));
        Tenant platformTenant = mockTenantWithId(1L);
        when(tenantRepository.findByTenantCodeAndActiveTrue(TenantConstants.SYSTEM_INDIVIDUAL_CODE))
                .thenReturn(Optional.of(platformTenant));
        when(voucherTemplateRepository.findByCodeAndTenantId("CODE", 1L))
                .thenReturn(Optional.of(validTemplate(1L)));
        when(tenantVoucherInventoryRepository.findByTenantIdAndVoucherTemplateIdForUpdate(1L, 1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.purchaseForTenant("CODE", 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Tenant mismatch"));
    }

    @Test
    void purchaseForTenant_existingInventory_addsStock() {
        stubOrgTenant(1L);
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(buildUser(1L)));
        Tenant platformTenant = mockTenantWithId(1L);
        when(tenantRepository.findByTenantCodeAndActiveTrue(TenantConstants.SYSTEM_INDIVIDUAL_CODE))
                .thenReturn(Optional.of(platformTenant));
        VoucherTemplate template = validTemplate(1L);
        when(voucherTemplateRepository.findByCodeAndTenantId("CODE", 1L))
                .thenReturn(Optional.of(template));

        TenantVoucherInventory inventory = new TenantVoucherInventory(1L, template, 5);
        when(tenantVoucherInventoryRepository.findByTenantIdAndVoucherTemplateIdForUpdate(1L, template.getId()))
                .thenReturn(Optional.of(inventory));
        when(tenantVoucherInventoryRepository.save(inventory)).thenReturn(inventory);

        TenantVoucherInventory saved = service.purchaseForTenant("CODE", 3, 10L, 1L);

        assertSame(inventory, saved);
        assertEquals(8, saved.getQuantityPurchasedTotal());
        assertEquals(8, saved.getQuantityAvailable());
        verify(tenantVoucherInventoryRepository).save(inventory);
    }

    @Test
    void purchaseForTenant_newInventory_creates() {
        stubOrgTenant(1L);
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(buildUser(1L)));
        Tenant platformTenant = mockTenantWithId(1L);
        when(tenantRepository.findByTenantCodeAndActiveTrue(TenantConstants.SYSTEM_INDIVIDUAL_CODE))
                .thenReturn(Optional.of(platformTenant));
        VoucherTemplate template = validTemplate(1L);
        when(voucherTemplateRepository.findByCodeAndTenantId("CODE", 1L))
                .thenReturn(Optional.of(template));
        when(tenantVoucherInventoryRepository.findByTenantIdAndVoucherTemplateIdForUpdate(1L, template.getId()))
                .thenReturn(Optional.empty());
        when(tenantVoucherInventoryRepository.save(any(TenantVoucherInventory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TenantVoucherInventory saved = service.purchaseForTenant("CODE", 2, 10L, 1L);

        assertEquals(2, saved.getQuantityPurchasedTotal());
        assertEquals(2, saved.getQuantityAvailable());
        verify(tenantVoucherInventoryRepository).save(any(TenantVoucherInventory.class));
    }

    @Test
    void distributeToUser_nullInventoryId_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.distributeToUser(null, 2L, 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Inventory id is required"));
    }

    @Test
    void distributeToUser_nullTargetUserId_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.distributeToUser(1L, null, 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("User id is required"));
    }

    @Test
    void distributeToUser_invalidQuantity_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.distributeToUser(1L, 2L, 0, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Quantity must be greater than zero"));
    }

    @Test
    void distributeToUser_notOrganization_throws() {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getTenantType()).thenReturn(TenantType.SYSTEM_INDIVIDUAL);
        when(tenantRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(tenant));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.distributeToUser(1L, 2L, 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Operation allowed only for organization tenants"));
    }

    @Test
    void distributeToUser_distributorNotFound_throws() {
        stubOrgTenant(1L);
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.distributeToUser(1L, 2L, 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Tenant admin user not found"));
    }

    @Test
    void distributeToUser_targetUserNotFound_throws() {
        stubOrgTenant(1L);
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(buildUser(1L)));
        when(userRepository.findByIdAndTenantId(2L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.distributeToUser(1L, 2L, 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Target user not found in tenant"));
    }

    @Test
    void distributeToUser_inventoryNotFound_throws() {
        stubOrgTenant(1L);
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(buildUser(1L)));
        when(userRepository.findByIdAndTenantId(2L, 1L)).thenReturn(Optional.of(buildUser(1L)));
        when(tenantVoucherInventoryRepository.findByIdAndTenantIdForUpdate(1L, 1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.distributeToUser(1L, 2L, 1, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Tenant voucher inventory not found"));
    }

    @Test
    void distributeToUser_insufficientStock_throws() {
        stubOrgTenant(1L);
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(buildUser(1L)));
        when(userRepository.findByIdAndTenantId(2L, 1L)).thenReturn(Optional.of(buildUser(1L)));

        VoucherTemplate template = validTemplate(1L);
        TenantVoucherInventory inventory = new TenantVoucherInventory(1L, template, 1);
        when(tenantVoucherInventoryRepository.findByIdAndTenantIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(inventory));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.distributeToUser(1L, 2L, 5, 10L, 1L)
        );
        assertTrue(ex.getMessage().contains("Insufficient tenant voucher stock"));
    }

    @Test
    void distributeToUser_success_createsDistributionAndVoucher() {
        stubOrgTenant(1L);
        User distributor = buildUser(1L);
        User targetUser = buildUser(1L);
        when(userRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(distributor));
        when(userRepository.findByIdAndTenantId(2L, 1L)).thenReturn(Optional.of(targetUser));

        VoucherTemplate template = validTemplate(1L);
        TenantVoucherInventory inventory = new TenantVoucherInventory(1L, template, 10);
        when(tenantVoucherInventoryRepository.findByIdAndTenantIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(tenantVoucherInventoryRepository.save(inventory)).thenReturn(inventory);
        when(userVoucherRepository.save(any(UserVoucher.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tenantVoucherDistributionRepository.save(any(TenantVoucherDistribution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TenantVoucherDistribution saved = service.distributeToUser(1L, 2L, 3, 10L, 1L);

        assertEquals(7, inventory.getQuantityAvailable());
        ArgumentCaptor<UserVoucher> voucherCaptor = ArgumentCaptor.forClass(UserVoucher.class);
        verify(userVoucherRepository).save(voucherCaptor.capture());
        UserVoucher voucher = voucherCaptor.getValue();
        assertEquals(new BigDecimal("30.00"), voucher.getTotalPurchasedAmount());

        assertEquals(new BigDecimal("30.00"), saved.getTotalDistributedAmount());
        assertEquals(3, saved.getQuantityDistributed());
    }

    @Test
    void listTenantInventory_notOrganization_throws() {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getTenantType()).thenReturn(TenantType.SYSTEM_INDIVIDUAL);
        when(tenantRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(tenant));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.listTenantInventory(1L)
        );
        assertTrue(ex.getMessage().contains("Operation allowed only for organization tenants"));
    }

    @Test
    void listTenantInventory_returnsList() {
        stubOrgTenant(1L);
        List<TenantVoucherInventory> inventory = List.of(mock(TenantVoucherInventory.class));
        when(tenantVoucherInventoryRepository.findByTenantIdOrderByUpdatedAtDesc(1L)).thenReturn(inventory);

        List<TenantVoucherInventory> result = service.listTenantInventory(1L);

        assertSame(inventory, result);
        verify(tenantVoucherInventoryRepository).findByTenantIdOrderByUpdatedAtDesc(1L);
    }

    private void stubOrgTenant(Long tenantId) {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getTenantType()).thenReturn(TenantType.ORGANIZATION);
        when(tenantRepository.findByIdAndActiveTrue(tenantId)).thenReturn(Optional.of(tenant));
    }

    private Tenant mockTenantWithId(Long id) {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(id);
        return tenant;
    }

    private User buildUser(Long tenantId) {
        User user = new User(
                "Test",
                "User",
                "hash",
                "1234567890",
                "test@example.com",
                LocalDateTime.now()
        );
        user.setTenantId(tenantId);
        return user;
    }

    private VoucherTemplate validTemplate(Long tenantId) {
        VoucherTemplate template = new VoucherTemplate(
                "CODE",
                new BigDecimal("10.00"),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(10)
        );
        template.setTenantId(tenantId);
        setTemplateId(template, 1L);
        return template;
    }

    private void setTemplateId(VoucherTemplate template, Long id) {
        try {
            java.lang.reflect.Field field = VoucherTemplate.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(template, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
