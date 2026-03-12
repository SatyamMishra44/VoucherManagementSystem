package com.example.Voucher.platform;

import com.example.Voucher.dto.platform.CreateTenantAdminRequestDto;
import com.example.Voucher.dto.platform.TenantAdminResponseDto;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.User;
import com.example.Voucher.repository.RoleRepository;
import com.example.Voucher.repository.TenantAuditLogRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.security.RoleProperties;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantRepository;
import com.example.Voucher.tenant.TenantType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformTenantManagementService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleProperties roleProperties;
    private final PasswordEncoder passwordEncoder;
    private final TenantAuditLogRepository tenantAuditLogRepository;

    public PlatformTenantManagementService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            RoleProperties roleProperties,
            PasswordEncoder passwordEncoder,
            TenantAuditLogRepository tenantAuditLogRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.roleProperties = roleProperties;
        this.passwordEncoder = passwordEncoder;
        this.tenantAuditLogRepository = tenantAuditLogRepository;
    }

    @Transactional
    public TenantAdminResponseDto createTenantAdmin(
            Long tenantId,
            CreateTenantAdminRequestDto request,
            Long actorUserId
    ) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        if (!tenant.isActive()) {
            throw new IllegalArgumentException("Tenant is inactive");
        }
        if (!TenantType.ORGANIZATION.equals(tenant.getTenantType())) {
            throw new IllegalArgumentException("Tenant admin can be created only for organization tenants");
        }
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new IllegalArgumentException("Admin email already in use");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Admin phone number already in use");
        }

        Role tenantAdminRole = roleRepository.findByName(roleProperties.getTenantAdmin())
                .orElseThrow(() -> new IllegalStateException("TENANT_ADMIN role not configured"));

        User tenantAdmin = new User(
                request.getFirstName().trim(),
                request.getLastName().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getPhoneNumber(),
                request.getEmail().trim(),
                LocalDateTime.now()
        );
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.addRole(tenantAdminRole);
        tenantAdmin = userRepository.save(tenantAdmin);

        addAuditLog(
                tenantId,
                actorUserId,
                "TENANT_ADMIN_CREATED",
                "Tenant admin userId=" + tenantAdmin.getId()
        );

        return new TenantAdminResponseDto(
                tenantAdmin.getId(),
                tenantId,
                tenantAdmin.getEmail(),
                tenantAdmin.getPhoneNumber(),
                tenantAdmin.getCreatedAt()
        );
    }

    @Transactional
    public Tenant updateTenantActiveStatus(Long tenantId, boolean active, String reason, Long actorUserId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        if (TenantType.SYSTEM_INDIVIDUAL.equals(tenant.getTenantType()) && !active) {
            throw new IllegalArgumentException("SYSTEM_INDIVIDUAL tenant cannot be deactivated");
        }

        if (active) {
            tenant.activate();
        } else {
            tenant.deactivate();
        }
        Tenant saved = tenantRepository.save(tenant);

        addAuditLog(
                tenantId,
                actorUserId,
                active ? "TENANT_ACTIVATED" : "TENANT_DEACTIVATED",
                reason == null || reason.isBlank() ? "No reason provided" : reason.trim()
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Tenant> listTenants() {
        return tenantRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<TenantAuditLog> getTenantAuditLogs(Long tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new IllegalArgumentException("Tenant not found");
        }
        return tenantAuditLogRepository.findTop100ByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    private void addAuditLog(Long tenantId, Long actorUserId, String action, String details) {
        tenantAuditLogRepository.save(new TenantAuditLog(tenantId, actorUserId, action, details));
    }
}
