package com.example.Voucher.controller;

import com.example.Voucher.dto.tenant.TenantCustomVoucherRequestCreateDto;
import com.example.Voucher.dto.tenant.TenantCustomVoucherRequestResponseDto;
import com.example.Voucher.dto.tenant.TenantVoucherDistributeRequestDto;
import com.example.Voucher.dto.tenant.TenantVoucherDistributionResponseDto;
import com.example.Voucher.dto.tenant.TenantVoucherInventoryResponseDto;
import com.example.Voucher.dto.tenant.TenantVoucherPurchaseRequestDto;
import com.example.Voucher.entity.TenantVoucherRequest;
import com.example.Voucher.entity.TenantVoucherDistribution;
import com.example.Voucher.entity.TenantVoucherInventory;
import com.example.Voucher.service.CurrentUserService;
import com.example.Voucher.service.TenantVoucherAdministrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "8. Tenant Vouchers", description = "Tenant-admin APIs to buy platform vouchers and distribute to org users")
@RestController
@RequestMapping("/api/v1/tenant/vouchers")
public class TenantVoucherAdminController {

    private final TenantVoucherAdministrationService tenantVoucherAdministrationService;
    private final CurrentUserService currentUserService;

    public TenantVoucherAdminController(
            TenantVoucherAdministrationService tenantVoucherAdministrationService,
            CurrentUserService currentUserService
    ) {
        this.tenantVoucherAdministrationService = tenantVoucherAdministrationService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/purchase")
    @PreAuthorize("hasAuthority(@roleProperties.getTenantAdmin())")
    @Operation(
            summary = "Tenant Admin: Buy Vouchers From Platform Catalog",
            description = "Adds stock to tenant inventory using platform-managed voucher templates."
    )
    public ResponseEntity<TenantVoucherInventoryResponseDto> purchaseVoucherStock(
            @Valid @RequestBody TenantVoucherPurchaseRequestDto request
    ) {
        TenantVoucherInventory inventory = tenantVoucherAdministrationService.purchaseForTenant(
                request.getVoucherCode(),
                request.getQuantity(),
                currentUserService.getCurrentUserId(),
                currentUserService.getCurrentTenantId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantVoucherInventoryResponseDto.fromEntity(inventory));
    }

    @PostMapping("/distribute")
    @PreAuthorize("hasAuthority(@roleProperties.getTenantAdmin())")
    @Operation(
            summary = "Tenant Admin: Distribute Vouchers To User",
            description = "Consumes tenant stock and assigns voucher balance to a user within the same organization tenant."
    )
    public ResponseEntity<TenantVoucherDistributionResponseDto> distributeVoucher(
            @Valid @RequestBody TenantVoucherDistributeRequestDto request
    ) {
        TenantVoucherDistribution distribution = tenantVoucherAdministrationService.distributeToUser(
                request.getInventoryId(),
                request.getUserId(),
                request.getQuantity(),
                currentUserService.getCurrentUserId(),
                currentUserService.getCurrentTenantId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantVoucherDistributionResponseDto.fromEntity(distribution));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAuthority(@roleProperties.getTenantAdmin())")
    @Operation(
            summary = "Tenant Admin: View Tenant Voucher Inventory",
            description = "Lists current stock available for distribution in the organization tenant."
    )
    public ResponseEntity<List<TenantVoucherInventoryResponseDto>> getTenantInventory() {
        List<TenantVoucherInventoryResponseDto> inventory = tenantVoucherAdministrationService
                .listTenantInventory(currentUserService.getCurrentTenantId())
                .stream()
                .map(TenantVoucherInventoryResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(inventory);
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAuthority(@roleProperties.getTenantAdmin())")
    @Operation(
            summary = "Tenant Admin: Request Custom Voucher",
            description = "Creates a custom voucher request for platform admin approval and future template creation."
    )
    public ResponseEntity<TenantCustomVoucherRequestResponseDto> createCustomVoucherRequest(
            @Valid @RequestBody TenantCustomVoucherRequestCreateDto request
    ) {
        TenantVoucherRequest created = tenantVoucherAdministrationService.submitCustomVoucherRequest(
                request.getRequestedVoucherCode(),
                request.getRequestedUnitValue(),
                request.getRequestedStartDate(),
                request.getRequestedExpiryDate(),
                request.getNotes(),
                currentUserService.getCurrentUserId(),
                currentUserService.getCurrentTenantId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantCustomVoucherRequestResponseDto.fromEntity(created));
    }
}
