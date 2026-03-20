package com.example.Voucher.platform;

import com.example.Voucher.repository.TenantAuditLogRepository;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantRepository;
import com.example.Voucher.tenant.TenantType;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformTenantManagementService {

    private final TenantRepository tenantRepository;
    private final TenantAuditLogRepository tenantAuditLogRepository;

    public PlatformTenantManagementService(
            TenantRepository tenantRepository,
            TenantAuditLogRepository tenantAuditLogRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.tenantAuditLogRepository = tenantAuditLogRepository;
    }

    @Transactional
    public Tenant updateTenantActiveStatus(Long tenantId, boolean active, String reason, Long actorUserId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        if (TenantType.SYSTEM_INDIVIDUAL.equals(tenant.getTenantType()) && !active) {
            throw new IllegalArgumentException("SYSTEM_INDIVIDUAL tenant cannot be deactivated");
        }

        if (active) {
            tenant.activate();
        } else {
            tenant.deactivate();
        }
        Tenant saved = tenantRepository.save(tenant);

        addAuditLog(
                tenantId,
                actorUserId,
                active ? "TENANT_ACTIVATED" : "TENANT_DEACTIVATED",
                reason == null || reason.isBlank() ? "No reason provided" : reason.trim()
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Tenant> listTenants() {
        return tenantRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<TenantAuditLog> getTenantAuditLogs(Long tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new IllegalArgumentException("Tenant not found");
        }
        return tenantAuditLogRepository.findTop100ByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    private void addAuditLog(Long tenantId, Long actorUserId, String action, String details) {
        tenantAuditLogRepository.save(new TenantAuditLog(tenantId, actorUserId, action, details));
    }
}
