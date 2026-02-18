package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.repository.VoucherTemplateRepository;
import com.example.Voucher.service.VoucherTemplateService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class VoucherTemplateServiceImpl implements VoucherTemplateService {

    private final VoucherTemplateRepository voucherTemplateRepository;

    public VoucherTemplateServiceImpl(VoucherTemplateRepository voucherTemplateRepository) {
        this.voucherTemplateRepository = voucherTemplateRepository;
    }

    @Override
    public VoucherTemplate createTemplate(VoucherTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("Voucher template cannot be null");
        }
        if (template.getStartDate().isAfter(template.getExpiryDate())) {
            throw new IllegalArgumentException("Start date cannot be after expiry date");
        }
        if (voucherTemplateRepository.findByCode(template.getCode()).isPresent()) {
            throw new IllegalArgumentException("Voucher code already exists");
        }
        return voucherTemplateRepository.save(template);
    }

    @Override
    public VoucherTemplate updateTemplateStatus(Long templateId, boolean enabled) {
        VoucherTemplate template = voucherTemplateRepository.findById(templateId)
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
        LocalDate today = LocalDate.now();
        return voucherTemplateRepository
                .findByEnabledTrueAndStartDateLessThanEqualAndExpiryDateGreaterThanEqual(today, today);
    }
}
