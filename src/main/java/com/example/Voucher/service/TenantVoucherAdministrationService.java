package com.example.Voucher.service;

import com.example.Voucher.entity.TenantVoucherDistribution;
import com.example.Voucher.entity.TenantVoucherInventory;
import java.util.List;

public interface TenantVoucherAdministrationService {

    TenantVoucherInventory purchaseForTenant(String voucherCode, Integer quantity, Long actorUserId, Long tenantId);

    TenantVoucherDistribution distributeToUser(Long inventoryId, Long targetUserId, Integer quantity, Long actorUserId, Long tenantId);

    List<TenantVoucherInventory> listTenantInventory(Long tenantId);
}
