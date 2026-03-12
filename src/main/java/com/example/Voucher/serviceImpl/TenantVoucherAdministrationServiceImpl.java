package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.TenantVoucherDistribution;
import com.example.Voucher.entity.TenantVoucherInventory;
import com.example.Voucher.entity.TenantVoucherRequest;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.UserVoucher;
import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.repository.TenantVoucherDistributionRepository;
import com.example.Voucher.repository.TenantVoucherInventoryRepository;
import com.example.Voucher.repository.TenantVoucherRequestRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.repository.UserVoucherRepository;
import com.example.Voucher.repository.VoucherTemplateRepository;
import com.example.Voucher.service.TenantVoucherAdministrationService;
import com.example.Voucher.tenant.Tenant;
import com.example.Voucher.tenant.TenantConstants;
import com.example.Voucher.tenant.TenantRepository;
import com.example.Voucher.tenant.TenantType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TenantVoucherAdministrationServiceImpl implements TenantVoucherAdministrationService {

    private final TenantVoucherInventoryRepository tenantVoucherInventoryRepository;
    private final TenantVoucherDistributionRepository tenantVoucherDistributionRepository;
    private final VoucherTemplateRepository voucherTemplateRepository;
    private final UserRepository userRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final TenantRepository tenantRepository;
    private final TenantVoucherRequestRepository tenantVoucherRequestRepository;

    public TenantVoucherAdministrationServiceImpl(
            TenantVoucherInventoryRepository tenantVoucherInventoryRepository,
            TenantVoucherDistributionRepository tenantVoucherDistributionRepository,
            VoucherTemplateRepository voucherTemplateRepository,
            UserRepository userRepository,
            UserVoucherRepository userVoucherRepository,
            TenantRepository tenantRepository,
            TenantVoucherRequestRepository tenantVoucherRequestRepository
    ) {
        this.tenantVoucherInventoryRepository = tenantVoucherInventoryRepository;
        this.tenantVoucherDistributionRepository = tenantVoucherDistributionRepository;
        this.voucherTemplateRepository = voucherTemplateRepository;
        this.userRepository = userRepository;
        this.userVoucherRepository = userVoucherRepository;
        this.tenantRepository = tenantRepository;
        this.tenantVoucherRequestRepository = tenantVoucherRequestRepository;
    }

    @Override
    public TenantVoucherInventory purchaseForTenant(String voucherCode, Integer quantity, Long actorUserId, Long tenantId) {
        if (voucherCode == null || voucherCode.isBlank()) {
            throw new IllegalArgumentException("Voucher code is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        ensureOrganizationTenant(tenantId);
        User actor = userRepository.findByIdAndTenantId(actorUserId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant admin user not found"));

        Long platformTenantId = tenantRepository.findByTenantCodeAndActiveTrue(TenantConstants.SYSTEM_INDIVIDUAL_CODE)
                .map(Tenant::getId)
                .orElseThrow(() -> new IllegalArgumentException("Platform tenant not found"));

        VoucherTemplate template = voucherTemplateRepository.findByCodeAndTenantId(voucherCode, platformTenantId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher template not found"));
        validateTemplateEligibility(template);

        TenantVoucherInventory inventory = tenantVoucherInventoryRepository
                .findByTenantIdAndVoucherTemplateIdForUpdate(tenantId, template.getId())
                .map(existing -> {
                    existing.addStock(quantity);
                    return existing;
                })
                .orElseGet(() -> new TenantVoucherInventory(tenantId, template, quantity));

        // actor lookup ensures caller belongs to tenant.
        if (!actor.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Tenant mismatch");
        }

        return tenantVoucherInventoryRepository.save(inventory);
    }

    @Override
    public TenantVoucherDistribution distributeToUser(
            Long inventoryId,
            Long targetUserId,
            Integer quantity,
            Long actorUserId,
            Long tenantId
    ) {
        if (inventoryId == null) {
            throw new IllegalArgumentException("Inventory id is required");
        }
        if (targetUserId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        ensureOrganizationTenant(tenantId);
        User distributor = userRepository.findByIdAndTenantId(actorUserId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant admin user not found"));
        User targetUser = userRepository.findByIdAndTenantId(targetUserId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found in tenant"));

        TenantVoucherInventory inventory = tenantVoucherInventoryRepository.findByIdAndTenantIdForUpdate(inventoryId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant voucher inventory not found"));
        inventory.consumeStock(quantity);
        tenantVoucherInventoryRepository.save(inventory);

        BigDecimal quantityDecimal = BigDecimal.valueOf(quantity);
        BigDecimal totalAmount = inventory.getVoucherTemplate().getUnitValue()
                .multiply(quantityDecimal)
                .setScale(2, RoundingMode.HALF_UP);

        UserVoucher userVoucher = new UserVoucher(
                inventory.getVoucherTemplate(),
                targetUser,
                quantity,
                totalAmount,
                totalAmount
        );
        userVoucherRepository.save(userVoucher);

        TenantVoucherDistribution distribution = new TenantVoucherDistribution(
                tenantId,
                inventory,
                targetUser,
                distributor,
                quantity,
                totalAmount
        );
        return tenantVoucherDistributionRepository.save(distribution);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantVoucherInventory> listTenantInventory(Long tenantId) {
        ensureOrganizationTenant(tenantId);
        return tenantVoucherInventoryRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId);
    }

    @Override
    public TenantVoucherRequest submitCustomVoucherRequest(
            String requestedVoucherCode,
            BigDecimal requestedUnitValue,
            java.time.LocalDate requestedStartDate,
            java.time.LocalDate requestedExpiryDate,
            String notes,
            Long actorUserId,
            Long tenantId
    ) {
        if (requestedVoucherCode == null || requestedVoucherCode.isBlank()) {
            throw new IllegalArgumentException("Requested voucher code is required");
        }
        if (requestedUnitValue == null || requestedUnitValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Requested unit value must be greater than zero");
        }
        if (requestedStartDate == null || requestedExpiryDate == null) {
            throw new IllegalArgumentException("Requested date range is required");
        }
        if (requestedStartDate.isAfter(requestedExpiryDate)) {
            throw new IllegalArgumentException("Requested start date cannot be after expiry date");
        }

        ensureOrganizationTenant(tenantId);
        User requester = userRepository.findByIdAndTenantId(actorUserId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant admin user not found"));

        TenantVoucherRequest request = new TenantVoucherRequest(
                tenantId,
                requester,
                requestedVoucherCode.trim(),
                requestedUnitValue,
                requestedStartDate,
                requestedExpiryDate,
                notes == null ? null : notes.trim()
        );
        return tenantVoucherRequestRepository.save(request);
    }

    private void validateTemplateEligibility(VoucherTemplate template) {
        if (!template.isEnabled()) {
            throw new IllegalArgumentException("Voucher template is disabled");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(template.getStartDate()) || today.isAfter(template.getExpiryDate())) {
            throw new IllegalArgumentException("Voucher template is not valid on this date");
        }
    }

    private void ensureOrganizationTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findByIdAndActiveTrue(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        if (!TenantType.ORGANIZATION.equals(tenant.getTenantType())) {
            throw new IllegalArgumentException("Operation allowed only for organization tenants");
        }
    }
}
