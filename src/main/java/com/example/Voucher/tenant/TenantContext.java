package com.example.Voucher.tenant;

public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT_ID.set(tenantId);
    }

    public static Long getTenantId() {
        return CURRENT_TENANT_ID.get();
    }

    public static Long requireTenantId() {
        Long tenantId = getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant resolved for current request");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT_TENANT_ID.remove();
    }
}
