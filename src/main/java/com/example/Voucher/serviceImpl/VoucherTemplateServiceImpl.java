package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.repository.VoucherTemplateRepository;
import com.example.Voucher.service.VoucherTemplateService;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantConstants;
import com.example.Voucher.tenant.TenantContext;
import com.example.Voucher.tenant.TenantRepository;
import com.example.Voucher.tenant.TenantType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class VoucherTemplateServiceImpl implements VoucherTemplateService {

    private final VoucherTemplateRepository voucherTemplateRepository;
    private final TenantRepository tenantRepository;

    public VoucherTemplateServiceImpl(
            VoucherTemplateRepository voucherTemplateRepository,
            TenantRepository tenantRepository
    ) {
        this.voucherTemplateRepository = voucherTemplateRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public VoucherTemplate createTemplate(VoucherTemplate template) {
        Long tenantId = TenantContext.requireTenantId();
        if (template == null) {
            throw new IllegalArgumentException("Voucher template cannot be null");
        }
        if (template.getStartDate().isAfter(template.getExpiryDate())) {
            throw new IllegalArgumentException("Start date cannot be after expiry date");
        }
        if (voucherTemplateRepository.findByCodeAndTenantId(template.getCode(), tenantId).isPresent()) {
            throw new IllegalArgumentException("Voucher code already exists");
        }
        template.setTenantId(tenantId);
        return voucherTemplateRepository.save(template);
    }

    @Override
    public VoucherTemplate updateTemplateStatus(Long templateId, boolean enabled) {
        Long tenantId = TenantContext.requireTenantId();
        VoucherTemplate template = voucherTemplateRepository.findByIdAndTenantId(templateId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher template not found"));
        if (enabled) {
            template.enable();
        } else {
            template.disable();
        }
        return voucherTemplateRepository.save(template);
    }

    @Override
    public List<VoucherTemplate> getEligibleTemplates() {
        Long currentTenantId = TenantContext.requireTenantId();
        Tenant currentTenant = tenantRepository.findByIdAndActiveTrue(currentTenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        Long catalogTenantId = currentTenantId;
        if (TenantType.ORGANIZATION.equals(currentTenant.getTenantType())) {
            catalogTenantId = tenantRepository.findByTenantCodeAndActiveTrue(TenantConstants.SYSTEM_INDIVIDUAL_CODE)
                    .map(Tenant::getId)
                    .orElseThrow(() -> new IllegalArgumentException("Platform tenant not found"));
        }

        LocalDate today = LocalDate.now();
        return voucherTemplateRepository
                .findByTenantIdAndEnabledTrueAndStartDateLessThanEqualAndExpiryDateGreaterThanEqual(
                        catalogTenantId,
                        today,
                        today
                );
    }
}
