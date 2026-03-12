package com.example.Voucher.repository;

import com.example.Voucher.entity.TenantVoucherRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantVoucherRequestRepository extends JpaRepository<TenantVoucherRequest, Long> {

    List<TenantVoucherRequest> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
