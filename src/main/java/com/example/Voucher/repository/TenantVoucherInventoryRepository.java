package com.example.Voucher.repository;

import com.example.Voucher.entity.TenantVoucherInventory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantVoucherInventoryRepository extends JpaRepository<TenantVoucherInventory, Long> {

    List<TenantVoucherInventory> findByTenantIdOrderByUpdatedAtDesc(Long tenantId);

    Optional<TenantVoucherInventory> findByTenantIdAndVoucherTemplateId(Long tenantId, Long voucherTemplateId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select tvi from TenantVoucherInventory tvi
            where tvi.tenantId = :tenantId
              and tvi.voucherTemplate.id = :voucherTemplateId
            """)
    Optional<TenantVoucherInventory> findByTenantIdAndVoucherTemplateIdForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("voucherTemplateId") Long voucherTemplateId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select tvi from TenantVoucherInventory tvi
            where tvi.id = :id
              and tvi.tenantId = :tenantId
            """)
    Optional<TenantVoucherInventory> findByIdAndTenantIdForUpdate(
            @Param("id") Long id,
            @Param("tenantId") Long tenantId
    );
}
