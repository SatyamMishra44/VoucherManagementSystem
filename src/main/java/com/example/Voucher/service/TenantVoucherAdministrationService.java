package com.example.Voucher.service;

import com.example.Voucher.entity.TenantVoucherDistribution;
import com.example.Voucher.entity.TenantVoucherInventory;
import com.example.Voucher.entity.TenantVoucherRequest;
import java.util.List;

public interface TenantVoucherAdministrationService {

    TenantVoucherInventory purchaseForTenant(String voucherCode, Integer quantity, Long actorUserId, Long tenantId);

    TenantVoucherDistribution distributeToUser(Long inventoryId, Long targetUserId, Integer quantity, Long actorUserId, Long tenantId);

    List<TenantVoucherInventory> listTenantInventory(Long tenantId);

    TenantVoucherRequest submitCustomVoucherRequest(
            String requestedVoucherCode,
            java.math.BigDecimal requestedUnitValue,
            java.time.LocalDate requestedStartDate,
            java.time.LocalDate requestedExpiryDate,
            String notes,
            Long actorUserId,
            Long tenantId
    );
}
