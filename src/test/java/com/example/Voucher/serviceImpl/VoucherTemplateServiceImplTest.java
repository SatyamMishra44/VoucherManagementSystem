package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.repository.VoucherTemplateRepository;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantConstants;
import com.example.Voucher.tenant.TenantContext;
import com.example.Voucher.tenant.TenantRepository;
import com.example.Voucher.tenant.TenantType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherTemplateServiceImplTest {

    @Mock
    private VoucherTemplateRepository voucherTemplateRepository;

    @Mock
    private TenantRepository tenantRepository;

    private VoucherTemplateServiceImpl voucherTemplateService;

    @BeforeEach
    void setUp() {
        voucherTemplateService = new VoucherTemplateServiceImpl(voucherTemplateRepository, tenantRepository);
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createTemplate_nullTemplate_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> voucherTemplateService.createTemplate(null)
        );
        assertTrue(ex.getMessage().contains("Voucher template cannot be null"));
        verifyNoInteractions(voucherTemplateRepository);
    }

    @Test
    void createTemplate_startDateAfterExpiry_throws() {
        VoucherTemplate template = new VoucherTemplate(
                "V001",
                new BigDecimal("10.00"),
                LocalDate.now().plusDays(1),
                LocalDate.now()
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> voucherTemplateService.createTemplate(template)
        );
        assertTrue(ex.getMessage().contains("Start date cannot be after expiry date"));
        verifyNoInteractions(voucherTemplateRepository);
    }

    @Test
    void createTemplate_duplicateCode_throws() {
        VoucherTemplate template = validTemplate();
        when(voucherTemplateRepository.findByCodeAndTenantId("V001", 1L))
                .thenReturn(Optional.of(template));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> voucherTemplateService.createTemplate(template)
        );
        assertTrue(ex.getMessage().contains("Voucher code already exists"));
        verify(voucherTemplateRepository).findByCodeAndTenantId("V001", 1L);
    }

    @Test
    void createTemplate_valid_setsTenantId_andSaves() {
        VoucherTemplate template = validTemplate();
        when(voucherTemplateRepository.findByCodeAndTenantId("V001", 1L))
                .thenReturn(Optional.empty());
        when(voucherTemplateRepository.save(template)).thenReturn(template);

        VoucherTemplate saved = voucherTemplateService.createTemplate(template);

        assertSame(template, saved);
        assertEquals(1L, template.getTenantId());
        verify(voucherTemplateRepository).save(template);
    }

    @Test
    void updateTemplateStatus_templateNotFound_throws() {
        when(voucherTemplateRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> voucherTemplateService.updateTemplateStatus(5L, true)
        );
        assertTrue(ex.getMessage().contains("Voucher template not found"));
    }

    @Test
    void updateTemplateStatus_enableTrue_enablesAndSaves() {
        VoucherTemplate template = validTemplate();
        template.disable();
        when(voucherTemplateRepository.findByIdAndTenantId(5L, 1L))
                .thenReturn(Optional.of(template));
        when(voucherTemplateRepository.save(template)).thenReturn(template);

        VoucherTemplate saved = voucherTemplateService.updateTemplateStatus(5L, true);

        assertSame(template, saved);
        assertTrue(saved.isEnabled());
        verify(voucherTemplateRepository).save(template);
    }

    @Test
    void updateTemplateStatus_enableFalse_disablesAndSaves() {
        VoucherTemplate template = validTemplate();
        when(voucherTemplateRepository.findByIdAndTenantId(6L, 1L))
                .thenReturn(Optional.of(template));
        when(voucherTemplateRepository.save(template)).thenReturn(template);

        VoucherTemplate saved = voucherTemplateService.updateTemplateStatus(6L, false);

        assertSame(template, saved);
        assertFalse(saved.isEnabled());
        verify(voucherTemplateRepository).save(template);
    }

    @Test
    void getEligibleTemplates_tenantNotFound_throws() {
        when(tenantRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> voucherTemplateService.getEligibleTemplates()
        );
        assertTrue(ex.getMessage().contains("Tenant not found"));
    }

    @Test
    void getEligibleTemplates_organizationTenant_usesPlatformTenant() {
        Tenant orgTenant = mock(Tenant.class);
        when(orgTenant.getTenantType()).thenReturn(TenantType.ORGANIZATION);
        when(tenantRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(orgTenant));

        Tenant platformTenant = mock(Tenant.class);
        when(platformTenant.getId()).thenReturn(99L);
        when(tenantRepository.findByTenantCodeAndActiveTrue(TenantConstants.SYSTEM_INDIVIDUAL_CODE))
                .thenReturn(Optional.of(platformTenant));

        LocalDate today = LocalDate.now();
        List<VoucherTemplate> templates = List.of(validTemplate());
        when(voucherTemplateRepository
                .findByTenantIdAndEnabledTrueAndStartDateLessThanEqualAndExpiryDateGreaterThanEqual(
                        99L,
                        today,
                        today
                ))
                .thenReturn(templates);

        List<VoucherTemplate> result = voucherTemplateService.getEligibleTemplates();

        assertSame(templates, result);
        verify(voucherTemplateRepository)
                .findByTenantIdAndEnabledTrueAndStartDateLessThanEqualAndExpiryDateGreaterThanEqual(
                        99L,
                        today,
                        today
                );
    }

    @Test
    void getEligibleTemplates_individualTenant_usesCurrentTenant() {
        Tenant individualTenant = mock(Tenant.class);
        when(individualTenant.getTenantType()).thenReturn(TenantType.SYSTEM_INDIVIDUAL);
        when(tenantRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(individualTenant));

        LocalDate today = LocalDate.now();
        List<VoucherTemplate> templates = List.of(validTemplate());
        when(voucherTemplateRepository
                .findByTenantIdAndEnabledTrueAndStartDateLessThanEqualAndExpiryDateGreaterThanEqual(
                        1L,
                        today,
                        today
                ))
                .thenReturn(templates);

        List<VoucherTemplate> result = voucherTemplateService.getEligibleTemplates();

        assertSame(templates, result);
        verify(voucherTemplateRepository)
                .findByTenantIdAndEnabledTrueAndStartDateLessThanEqualAndExpiryDateGreaterThanEqual(
                        1L,
                        today,
                        today
                );
    }

    private VoucherTemplate validTemplate() {
        return new VoucherTemplate(
                "V001",
                new BigDecimal("10.00"),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(10)
        );
    }
}
