package com.example.Voucher.repository;

import com.example.Voucher.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByUserIdAndTenantId(Long userId, Long tenantId);

    Optional<Bill> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Bill> findByIdAndUserIdAndTenantId(Long id, Long userId, Long tenantId);

    Optional<Bill> findByRequestIdAndTenantId(String requestId, Long tenantId);
}
