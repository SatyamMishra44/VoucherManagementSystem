package com.example.Voucher.tenant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByTenantCodeAndActiveTrue(String tenantCode);
    Optional<Tenant> findByTenantCode(String tenantCode);

    Optional<Tenant> findByIdAndActiveTrue(Long id);

    java.util.List<Tenant> findAllByOrderByCreatedAtDesc();
}
