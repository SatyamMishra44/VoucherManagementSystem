package com.example.Voucher.repository;

import com.example.Voucher.entity.VoucherTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherTemplateRepository extends JpaRepository<VoucherTemplate, Long> {

    Optional<VoucherTemplate> findByCodeAndTenantId(String code, Long tenantId);

    Optional<VoucherTemplate> findByIdAndTenantId(Long id, Long tenantId);

    List<VoucherTemplate> findByTenantIdAndEnabledTrueAndStartDateLessThanEqualAndExpiryDateGreaterThanEqual(
            Long tenantId,
            LocalDate startDate,
            LocalDate expiryDate
    );
}
