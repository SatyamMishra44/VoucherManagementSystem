package com.example.Voucher.repository;

import com.example.Voucher.entity.TenantVoucherDistribution;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantVoucherDistributionRepository extends JpaRepository<TenantVoucherDistribution, Long> {

    List<TenantVoucherDistribution> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
