package com.example.Voucher.repository;

import com.example.Voucher.entity.RedemptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedemptionHistoryRepository extends JpaRepository<RedemptionHistory, Long> {

    List<RedemptionHistory> findByUserVoucherUserIdOrderByRedeemedAtDesc(Long userId);

    List<RedemptionHistory> findByUserVoucherIdOrderByRedeemedAtDesc(Long userVoucherId);
}
