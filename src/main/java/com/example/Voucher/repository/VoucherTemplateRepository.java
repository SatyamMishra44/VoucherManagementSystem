package com.example.Voucher.repository;

import com.example.Voucher.entity.VoucherTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherTemplateRepository extends JpaRepository<VoucherTemplate, Long> {

    Optional<VoucherTemplate> findByCode(String code);

    List<VoucherTemplate> findByEnabledTrueAndStartDateLessThanEqualAndExpiryDateGreaterThanEqual(
            LocalDate startDate,
            LocalDate expiryDate
    );
}
