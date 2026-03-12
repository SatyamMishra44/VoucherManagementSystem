package com.example.Voucher.serviceImpl;

import com.example.Voucher.dto.onboarding.TenantOnboardingRequestResponseDto;
import com.example.Voucher.dto.onboarding.TenantSignupRequestDto;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.TenantOnboardingRequest;
import com.example.Voucher.entity.TenantOnboardingStatus;
import com.example.Voucher.entity.User;
import com.example.Voucher.platform.TenantAuditLog;
import com.example.Voucher.repository.RoleRepository;
import com.example.Voucher.repository.TenantAuditLogRepository;
import com.example.Voucher.repository.TenantOnboardingRequestRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.security.RoleProperties;
import com.example.Voucher.service.TenantOnboardingService;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantRepository;
import com.example.Voucher.tenant.TenantType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantOnboardingServiceImpl implements TenantOnboardingService {

    private static final Set<TenantOnboardingStatus> BLOCKING_REQUEST_STATUSES =
            Set.of(TenantOnboardingStatus.PENDING, TenantOnboardingStatus.APPROVED);

    private final TenantOnboardingRequestRepository tenantOnboardingRequestRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleProperties roleProperties;
    private final PasswordEncoder passwordEncoder;
    private final TenantAuditLogRepository tenantAuditLogRepository;

    public TenantOnboardingServiceImpl(
            TenantOnboardingRequestRepository tenantOnboardingRequestRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            RoleProperties roleProperties,
            PasswordEncoder passwordEncoder,
            TenantAuditLogRepository tenantAuditLogRepository
    ) {
        this.tenantOnboardingRequestRepository = tenantOnboardingRequestRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.roleProperties = roleProperties;
        this.passwordEncoder = passwordEncoder;
        this.tenantAuditLogRepository = tenantAuditLogRepository;
    }

    @Override
    @Transactional
    public TenantOnboardingRequestResponseDto submitOnboardingRequest(TenantSignupRequestDto request) {
        String tenantCode = normalizeTenantCode(request.getTenantCode());
        String tenantName = request.getTenantName().trim();
        String adminEmail = request.getAdminEmail().trim().toLowerCase();
        String adminPhone = request.getAdminPhoneNumber().trim();

        validateRequestIsUnique(tenantCode, adminEmail, adminPhone);

        TenantOnboardingRequest onboardingRequest = new TenantOnboardingRequest(
                tenantName,
                tenantCode,
                request.getAdminFirstName().trim(),
                request.getAdminLastName().trim(),
                adminEmail,
                adminPhone,
                passwordEncoder.encode(request.getAdminPassword()),
                trimToNull(request.getNotes())
        );
        onboardingRequest = tenantOnboardingRequestRepository.save(onboardingRequest);
        return TenantOnboardingRequestResponseDto.fromEntity(onboardingRequest);
    }

    @Override
    @Transactional
    public TenantOnboardingRequestResponseDto reviewRequest(Long requestId, boolean approved, String comment, Long actorUserId) {
        TenantOnboardingRequest request = tenantOnboardingRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding request not found"));

        if (request.getStatus() != TenantOnboardingStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be reviewed");
        }

        String reviewComment = trimToNull(comment);
        if (!approved && reviewComment == null) {
            throw new IllegalArgumentException("Rejection comment is required");
        }

        if (!approved) {
            request.markRejected(actorUserId, reviewComment);
            request = tenantOnboardingRequestRepository.save(request);
            return TenantOnboardingRequestResponseDto.fromEntity(request);
        }

        validateApprovalIsUnique(request);

        Role tenantAdminRole = roleRepository.findByName(roleProperties.getTenantAdmin())
                .orElseThrow(() -> new IllegalStateException("TENANT_ADMIN role not configured"));

        Tenant tenant = tenantRepository.save(new Tenant(
                request.getTenantCode(),
                request.getTenantName(),
                TenantType.ORGANIZATION
        ));

        User tenantAdmin = new User(
                request.getAdminFirstName(),
                request.getAdminLastName(),
                request.getAdminPasswordHash(),
                request.getAdminPhoneNumber(),
                request.getAdminEmail(),
                LocalDateTime.now()
        );
        tenantAdmin.setTenantId(tenant.getId());
        tenantAdmin.addRole(tenantAdminRole);
        tenantAdmin = userRepository.save(tenantAdmin);

        request.markApproved(actorUserId, reviewComment, tenant.getId(), tenantAdmin.getId());
        request = tenantOnboardingRequestRepository.save(request);

        tenantAuditLogRepository.save(new TenantAuditLog(
                tenant.getId(),
                actorUserId,
                "TENANT_ONBOARDING_APPROVED",
                "Onboarding requestId=" + request.getId() + ", tenantAdminUserId=" + tenantAdmin.getId()
        ));

        return TenantOnboardingRequestResponseDto.fromEntity(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantOnboardingRequestResponseDto> listRequests(TenantOnboardingStatus status) {
        List<TenantOnboardingRequest> requests = status == null
                ? tenantOnboardingRequestRepository.findAllByOrderByCreatedAtDesc()
                : tenantOnboardingRequestRepository.findByStatusOrderByCreatedAtDesc(status);
        return requests.stream()
                .map(TenantOnboardingRequestResponseDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantOnboardingRequestResponseDto getRequest(Long requestId) {
        TenantOnboardingRequest request = tenantOnboardingRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding request not found"));
        return TenantOnboardingRequestResponseDto.fromEntity(request);
    }

    private void validateRequestIsUnique(String tenantCode, String adminEmail, String adminPhone) {
        if (tenantRepository.findByTenantCode(tenantCode).isPresent()) {
            throw new IllegalArgumentException("Tenant code already exists");
        }
        if (userRepository.existsByEmail(adminEmail)) {
            throw new IllegalArgumentException("Admin email already in use");
        }
        if (userRepository.existsByPhoneNumber(adminPhone)) {
            throw new IllegalArgumentException("Admin phone number already in use");
        }
        if (tenantOnboardingRequestRepository.existsByTenantCodeAndStatusIn(tenantCode, BLOCKING_REQUEST_STATUSES)) {
            throw new IllegalArgumentException("Tenant code already has an active onboarding request");
        }
        if (tenantOnboardingRequestRepository.existsByAdminEmailAndStatusIn(adminEmail, BLOCKING_REQUEST_STATUSES)) {
            throw new IllegalArgumentException("Admin email already has an active onboarding request");
        }
        if (tenantOnboardingRequestRepository.existsByAdminPhoneNumberAndStatusIn(adminPhone, BLOCKING_REQUEST_STATUSES)) {
            throw new IllegalArgumentException("Admin phone number already has an active onboarding request");
        }
    }

    private void validateApprovalIsUnique(TenantOnboardingRequest request) {
        String tenantCode = request.getTenantCode();
        String adminEmail = request.getAdminEmail();
        String adminPhone = request.getAdminPhoneNumber();
        Long requestId = request.getId();

        if (tenantRepository.findByTenantCode(tenantCode).isPresent()) {
            throw new IllegalArgumentException("Tenant code already exists");
        }
        if (userRepository.existsByEmail(adminEmail)) {
            throw new IllegalArgumentException("Admin email already in use");
        }
        if (userRepository.existsByPhoneNumber(adminPhone)) {
            throw new IllegalArgumentException("Admin phone number already in use");
        }
        if (tenantOnboardingRequestRepository.existsByTenantCodeAndStatusInAndIdNot(
                tenantCode, BLOCKING_REQUEST_STATUSES, requestId)) {
            throw new IllegalArgumentException("Tenant code already has an active onboarding request");
        }
        if (tenantOnboardingRequestRepository.existsByAdminEmailAndStatusInAndIdNot(
                adminEmail, BLOCKING_REQUEST_STATUSES, requestId)) {
            throw new IllegalArgumentException("Admin email already has an active onboarding request");
        }
        if (tenantOnboardingRequestRepository.existsByAdminPhoneNumberAndStatusInAndIdNot(
                adminPhone, BLOCKING_REQUEST_STATUSES, requestId)) {
            throw new IllegalArgumentException("Admin phone number already has an active onboarding request");
        }
    }

    private String normalizeTenantCode(String raw) {
        return raw.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
