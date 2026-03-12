package com.example.Voucher.repository;

import com.example.Voucher.platform.TenantAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantAuditLogRepository extends JpaRepository<TenantAuditLog, Long> {

    List<TenantAuditLog> findTop100ByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
