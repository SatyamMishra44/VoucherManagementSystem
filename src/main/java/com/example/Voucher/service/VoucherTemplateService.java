package com.example.Voucher.service;

import com.example.Voucher.entity.VoucherTemplate;

import java.util.List;

public interface VoucherTemplateService {

    VoucherTemplate createTemplate(VoucherTemplate template);

    VoucherTemplate updateTemplateStatus(Long templateId, boolean enabled);

    List<VoucherTemplate> getEligibleTemplates();
}
