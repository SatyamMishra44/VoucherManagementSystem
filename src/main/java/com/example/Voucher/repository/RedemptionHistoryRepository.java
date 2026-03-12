package com.example.Voucher.repository;

import com.example.Voucher.entity.RedemptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedemptionHistoryRepository extends JpaRepository<RedemptionHistory, Long> {

    List<RedemptionHistory> findByUserVoucherUserIdAndTenantIdOrderByRedeemedAtDesc(Long userId, Long tenantId);

    List<RedemptionHistory> findByUserVoucherIdAndTenantIdOrderByRedeemedAtDesc(Long userVoucherId, Long tenantId);
}
