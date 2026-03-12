package com.example.Voucher.repository;

import com.example.Voucher.entity.TenantOnboardingRequest;
import com.example.Voucher.entity.TenantOnboardingStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantOnboardingRequestRepository extends JpaRepository<TenantOnboardingRequest, Long> {

    List<TenantOnboardingRequest> findAllByOrderByCreatedAtDesc();

    List<TenantOnboardingRequest> findByStatusOrderByCreatedAtDesc(TenantOnboardingStatus status);

    boolean existsByTenantCodeAndStatusIn(String tenantCode, Collection<TenantOnboardingStatus> statuses);

    boolean existsByAdminEmailAndStatusIn(String adminEmail, Collection<TenantOnboardingStatus> statuses);

    boolean existsByAdminPhoneNumberAndStatusIn(String adminPhoneNumber, Collection<TenantOnboardingStatus> statuses);

    boolean existsByTenantCodeAndStatusInAndIdNot(
            String tenantCode,
            Collection<TenantOnboardingStatus> statuses,
            Long id
    );

    boolean existsByAdminEmailAndStatusInAndIdNot(
            String adminEmail,
            Collection<TenantOnboardingStatus> statuses,
            Long id
    );

    boolean existsByAdminPhoneNumberAndStatusInAndIdNot(
            String adminPhoneNumber,
            Collection<TenantOnboardingStatus> statuses,
            Long id
    );
}
