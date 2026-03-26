package com.example.Voucher.tenant;

// keeping this data in separate class will provide the separation concern
// follow the single responsibility principle
public final class TenantConstants {

    public static final String SYSTEM_INDIVIDUAL_CODE = "SYSTEM_INDIVIDUAL";
    public static final String TENANT_HEADER = "X-Tenant-Code";
    public static final String X_REQUEST_ID_HEADER = "X-Request-ID";

    private TenantConstants() {
    }
}
