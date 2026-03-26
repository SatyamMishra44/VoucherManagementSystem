package com.example.Voucher.tenant;


// TenanctContext is per-request storage that holds the current tenantId
public final class TenantContext {

    /*By the help of Thread Local each thread gets its own copy. so that data is not mixed between the thread.
    thread isolation is provided
    it is like a container that stores the value in key-value
    */


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
